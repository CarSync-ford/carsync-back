package br.com.sprint1.challenge.dto;

import br.com.sprint1.challenge.validation.LowercaseEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record AuthRequest(
        @NotBlank @Email @LowercaseEmail String email,
        @NotBlank @Size(min = 6, max = 20) String password
    ) {}

    public record AuthResponse(
        String token,
        String refreshToken
    ) {}

    public record RefreshTokenRequest(
        @NotBlank String refreshToken
    ) {}

    public record RefreshTokenResponse(
        String token,
        String refreshToken
    ) {}

    public record ForgotPasswordRequest(
        @NotBlank @Email @LowercaseEmail String email
    ) {}

    public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, max = 20) String newPassword
    ) {}

    public record ChangePasswordRequest(
        @NotBlank @Size(min = 6, max = 20) String currentPassword,
        @NotBlank @Size(min = 6, max = 20) String newPassword
    ) {}

    public record MfaEnableResponse(
        String secret,
        String qrCodeUri
    ) {}

    public record MfaVerifyRequest(
        @NotBlank @Size(min = 6, max = 6) String code
    ) {}
}