package br.com.sprint1.challenge.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import br.com.sprint1.challenge.service.TotpService;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final int bcryptRounds;
    private final int maxFailedAttempts = 5;
    private final int lockoutMinutes = 15;
    private String dummyHash;

    public AuthServiceImpl(
            UserRepository userRepository,
            JwtService jwtService,
            TotpService totpService,
            @Value("${spring.bcrypt.salt:10}") int bcryptRounds) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.bcryptRounds = bcryptRounds;
    }

    @PostConstruct
    public void init() {
        this.dummyHash = BCrypt.hashpw("__dummy__", BCrypt.gensalt(bcryptRounds));
    }

    @Override
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            // Anti-timing: perform dummy hash check
            BCrypt.checkpw(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();

        // Check if user is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UserLockedException(user.getLockedUntil());
        }

        if (!BCrypt.checkpw(request.password(), user.getHashedPassword())) {
            // Increment failed attempts
            handleFailedLogin(user);
            throw new InvalidCredentialsException();
        }

        // Successful login - reset failed attempts and lock
        userRepository.unlockUser(user.getId());
        userRepository.updateLastLoginById(user.getId());

        String role = user.getUserType() != null && user.getUserType().getType() != null
                ? user.getUserType().getType()
                : "USER";
        String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        LocalDateTime refreshTokenExpiry = LocalDateTime.now().plusDays(30);
        userRepository.updateRefreshToken(user.getId(), refreshToken, refreshTokenExpiry);

        return new AuthResponse(token, refreshToken);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= maxFailedAttempts) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
            userRepository.lockUser(user.getId(), lockedUntil);
        } else {
            userRepository.incrementFailedLoginAttempts(user.getId());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Validate refresh token matches and not expired
        if (!request.refreshToken().equals(user.getRefreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (user.getRefreshTokenExpiresAt() != null && user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }

        // Rotate: generate new tokens, invalidate old
        String role = user.getUserType() != null && user.getUserType().getType() != null
                ? user.getUserType().getType()
                : "USER";
        String newToken = jwtService.generateToken(user.getId(), user.getEmail(), role);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        LocalDateTime newRefreshTokenExpiry = LocalDateTime.now().plusDays(30);
        userRepository.updateRefreshToken(user.getId(), newRefreshToken, newRefreshTokenExpiry);

        return new RefreshTokenResponse(newToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        // Always return 202 to prevent email enumeration
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        String resetToken = jwtService.generatePasswordResetToken(user.getId());

        // TODO: Send email with resetToken (mock for now)
        // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        // In a real implementation, you would store the token hash in a password_reset_tokens table
        // For now, the token is self-contained in the JWT
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var claims = jwtService.parsePasswordResetToken(request.token());
        String userId = claims.getSubject();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Check if token is already used (single-use)
        // In a real implementation, check password_reset_tokens table

        // Update password
        String newHashedPassword = BCrypt.hashpw(request.newPassword(), BCrypt.gensalt(bcryptRounds));
        user.setHashedPassword(newHashedPassword);
        userRepository.save(user);

        // Revoke refresh token
        userRepository.revokeRefreshToken(userId);

        // TODO: Mark token as used in password_reset_tokens table
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!BCrypt.checkpw(request.currentPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException();
        }

        String newHashedPassword = BCrypt.hashpw(request.newPassword(), BCrypt.gensalt(bcryptRounds));
        user.setHashedPassword(newHashedPassword);
        userRepository.save(user);

        // Revoke refresh token on password change
        userRepository.revokeRefreshToken(userId);
    }

    @Override
    @Transactional
    public MfaEnableResponse enableMfa(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());

        // Generate TOTP secret
        String secret = totpService.generateSecret();
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
        boolean valid = totpService.verifyCode(user.getMfaSecret(), request.code());
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

}