package br.com.sprint1.challenge.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ForgotPasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.MfaEnableResponse;
import br.com.sprint1.challenge.dto.AuthDtos.MfaVerifyRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ResetPasswordRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.exception.InvalidTokenException;
import br.com.sprint1.challenge.exception.TokenExpiredException;
import br.com.sprint1.challenge.exception.UserLockedException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.service.AuthService;
import br.com.sprint1.challenge.service.JwtService;
import br.com.sprint1.challenge.service.PasswordResetEmailService;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final int bcryptRounds;
    private final int maxFailedAttempts = 5;
    private final int lockoutMinutes = 15;
    private String dummyHash;

    public AuthServiceImpl(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordResetEmailService passwordResetEmailService,
            JwtProperties jwtProperties,
            Clock clock,
            @Value("${spring.bcrypt.salt:10}") int bcryptRounds) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordResetEmailService = passwordResetEmailService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.bcryptRounds = bcryptRounds;
    }

    @PostConstruct
    public void init() {
        this.dummyHash = BCrypt.hashpw("__dummy__", BCrypt.gensalt(bcryptRounds));
    }

    @Override
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResponse authenticate(AuthRequest request) {
        var userOpt = userRepository.findByEmailForUpdate(request.email());

        if (userOpt.isEmpty()) {
            // Anti-timing: perform dummy hash check
            BCrypt.checkpw(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();

        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new UserLockedException(user.getLockedUntil());
        }
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!BCrypt.checkpw(request.password(), user.getHashedPassword())) {
            handleFailedLogin(user, now);
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(now);

        String role = user.getUserType() != null && user.getUserType().getType() != null
                ? user.getUserType().getType()
                : "USER";
        String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        LocalDateTime refreshTokenExpiry = LocalDateTime.now(clock)
                .plusDays(jwtProperties.getRefreshTokenExpiryDays());
        userRepository.updateRefreshToken(user.getId(), refreshToken, refreshTokenExpiry);

        return new AuthResponse(token, refreshToken);
    }

    private void handleFailedLogin(User user, LocalDateTime now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(now.plusMinutes(lockoutMinutes));
        }
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        // Parse the refresh token to get userId
        var claims = jwtService.parse(request.refreshToken());
        String tokenType = claims.get("type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new InvalidTokenException("Invalid token type");
        }

        String userId = claims.getSubject();
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Validate refresh token matches and not expired
        if (!request.refreshToken().equals(user.getRefreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (user.getRefreshTokenExpiresAt() == null
                || !user.getRefreshTokenExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new TokenExpiredException("Refresh token expired");
        }

        // Rotate: generate new tokens, invalidate old
        String role = user.getUserType() != null && user.getUserType().getType() != null
                ? user.getUserType().getType()
                : "USER";
        String newToken = jwtService.generateToken(user.getId(), user.getEmail(), role);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        LocalDateTime newRefreshTokenExpiry = LocalDateTime.now(clock)
                .plusDays(jwtProperties.getRefreshTokenExpiryDays());
        userRepository.updateRefreshToken(user.getId(), newRefreshToken, newRefreshTokenExpiry);

        return new RefreshTokenResponse(newToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmailForUpdate(request.email());

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        String resetToken = jwtService.generatePasswordResetToken(user.getId());
        user.setPasswordResetTokenHash(hashToken(resetToken));
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now(clock).plusMinutes(15));

        try {
            passwordResetEmailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (MailException ex) {
            clearPasswordReset(user);
            log.error("Password reset email delivery failed");
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var claims = jwtService.parsePasswordResetToken(request.token());
        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new InvalidTokenException("Invalid password reset token");
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));
        LocalDateTime now = LocalDateTime.now(clock);
        String storedHash = user.getPasswordResetTokenHash();
        if (storedHash == null
                || user.getPasswordResetTokenExpiresAt() == null
                || !user.getPasswordResetTokenExpiresAt().isAfter(now)
                || !MessageDigest.isEqual(
                        storedHash.getBytes(StandardCharsets.US_ASCII),
                        hashToken(request.token()).getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidTokenException("Invalid or expired password reset token");
        }
        if (claims.getExpiration() == null
                || !claims.getExpiration().toInstant().isAfter(clock.instant())) {
            throw new InvalidTokenException("Invalid or expired password reset token");
        }

        user.setHashedPassword(BCrypt.hashpw(request.newPassword(), BCrypt.gensalt(bcryptRounds)));
        clearPasswordReset(user);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!BCrypt.checkpw(request.currentPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException();
        }

        String newHashedPassword = BCrypt.hashpw(request.newPassword(), BCrypt.gensalt(bcryptRounds));
        user.setHashedPassword(newHashedPassword);
        clearPasswordReset(user);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
    }

    private void clearPasswordReset(User user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    @Override
    @Transactional
    public MfaEnableResponse enableMfa(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        // Generate TOTP secret
        String secret = generateTotpSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        // Generate QR code URI
        String qrCodeUri = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            "CarDealership",
            user.getEmail(),
            secret,
            "CarDealership"
        );

        return new MfaEnableResponse(secret, qrCodeUri);
    }

    @Override
    @Transactional
    public void verifyMfa(String userId, MfaVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (user.getMfaSecret() == null) {
            throw new InvalidTokenException("MFA not enabled");
        }

        // Verify TOTP code
        boolean valid = verifyTotpCode(user.getMfaSecret(), request.code());
        if (!valid) {
            throw new InvalidCredentialsException("Invalid MFA code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableMfa(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    private String generateTotpSecret() {
        // Base32 encoded secret for TOTP
        byte[] randomBytes = new byte[20];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return base32Encode(randomBytes);
    }

    private boolean verifyTotpCode(String secret, String code) {
        try {
            // Use the TOTP library for verification
            long timeWindow = System.currentTimeMillis() / 1000 / 30;
            return verifyTotp(secret, code, timeWindow);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyTotp(String secret, String code, long timeWindow) {
        try {
            // HMAC-SHA1 implementation for TOTP (RFC 6238)
            byte[] key = base32Decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = 0;
            for (int i = 0; i < 4; i++) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            int totp = truncatedHash % 1000000;
            String expectedCode = String.format("%06d", totp);

            // Allow 1 time window before/after for clock drift
            return expectedCode.equals(code)
                || String.format("%06d", ((truncatedHash + 1) % 1000000)).equals(code)
                || String.format("%06d", ((truncatedHash - 1 + 1000000) % 1000000)).equals(code);
        } catch (Exception e) {
            return false;
        }
    }

    private String base32Encode(byte[] data) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(alphabet.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(alphabet.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String encoded) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int bits = encoded.length() * 5;
        byte[] result = new byte[bits / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : encoded.toCharArray()) {
            int val = alphabet.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}