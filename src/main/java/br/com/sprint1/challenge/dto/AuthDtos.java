package br.com.sprint1.challenge.dto;

import br.com.sprint1.challenge.validation.LowercaseEmail;
import br.com.sprint1.challenge.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record AuthRequest(
        @NotBlank @Email @LowercaseEmail String email,
        @NotBlank @Size(min = 6, max = 20) String password,
        @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be 6 digits") String code
    ) {
        /** Backwards-compatible constructor for non-MFA logins and existing tests. */
        public AuthRequest(String email, String password) {
            this(email, password, null);
        }
    }

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
        @NotBlank @Size(max = 20) @StrongPassword String newPassword
    ) {}

    public record ChangePasswordRequest(
        @NotBlank @Size(min = 6, max = 20) String currentPassword,
        @NotBlank @Size(max = 20) @StrongPassword String newPassword
    ) {}

    public record MfaEnableResponse(
        String secret,
        String qrCodeUri
    ) {}

    public record MfaVerifyRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be 6 digits") String code
    ) {}

    public record MfaDisableRequest(
        @NotBlank @Size(min = 6, max = 20) String currentPassword,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be 6 digits") String code
    ) {}
}