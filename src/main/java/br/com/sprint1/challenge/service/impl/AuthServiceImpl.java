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
import br.com.sprint1.challenge.dto.AuthDtos.MfaDisableRequest;
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
import br.com.sprint1.challenge.util.TotpUtil;
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

        // MFA check: if enabled, require valid TOTP before issuing tokens
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            if (request.code() == null || request.code().isBlank()) {
                throw new InvalidCredentialsException("MFA code required");
            }
            long acceptedStep = TotpUtil.verify(
                    user.getMfaSecret(), request.code(), clock, user.getMfaLastUsedStep());
            if (acceptedStep < 0) {
                handleFailedLogin(user, now);
                throw new InvalidCredentialsException("Invalid MFA code");
            }
            user.setMfaLastUsedStep(acceptedStep);
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
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        // Don't overwrite active MFA secret
        if (Boolean.TRUE.equals(user.getMfaEnabled()) && user.getMfaSecret() != null) {
            throw new IllegalStateException("MFA is already active; disable it first");
        }

        String secret = TotpUtil.base32Encode(generateRandomBytes(20));
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);

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
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void verifyMfa(String userId, MfaVerifyRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (user.getMfaSecret() == null) {
            throw new InvalidTokenException("MFA not set up");
        }

        long acceptedStep = TotpUtil.verify(
                user.getMfaSecret(), request.code(), clock, user.getMfaLastUsedStep());
        if (acceptedStep < 0) {
            handleFailedLogin(user, LocalDateTime.now(clock));
            throw new InvalidCredentialsException("Invalid MFA code");
        }

        user.setMfaLastUsedStep(acceptedStep);
        user.setMfaEnabled(true);
        // Revoke refresh on activation
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
    }

    @Override
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void disableMfa(String userId, MfaDisableRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new InvalidTokenException("MFA is not enabled");
        }

        // Verify current password
        if (!BCrypt.checkpw(request.currentPassword(), user.getHashedPassword())) {
            handleFailedLogin(user, LocalDateTime.now(clock));
            throw new InvalidCredentialsException("Invalid password");
        }

        // Verify TOTP code
        long acceptedStep = TotpUtil.verify(
                user.getMfaSecret(), request.code(), clock, user.getMfaLastUsedStep());
        if (acceptedStep < 0) {
            handleFailedLogin(user, LocalDateTime.now(clock));
            throw new InvalidCredentialsException("Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaLastUsedStep(null);
        // Revoke refresh on deactivation
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
    }

    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }
}