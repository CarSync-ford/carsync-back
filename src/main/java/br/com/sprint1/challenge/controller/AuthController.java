package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ForgotPasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.MfaEnableResponse;
import br.com.sprint1.challenge.dto.AuthDtos.MfaVerifyRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ResetPasswordRequest;
import br.com.sprint1.challenge.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login with JSON payload", description = "Authenticates credentials and returns a JWT token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful - returns JWT token and refresh token"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<AuthResponse> loginJson(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.token())
                .body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Login with form-urlencoded payload", description = "Authenticates credentials (form) and returns a JWT token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful - returns JWT token and refresh token"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<AuthResponse> loginForm(@Valid @ModelAttribute AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.token())
                .body(response);
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and returns new access token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.token())
                .body(response);
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Request password reset", description = "Sends password reset email with a 15-minute single-use token. Always returns 202 to prevent email enumeration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Password reset email sent (if account exists)"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reset password with token", description = "Resets password using the token from the forgot-password email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change password", description = "Changes password for authenticated user. Revokes refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid current password"),
            @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/mfa/enable", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enable MFA", description = "Generates TOTP secret and QR code for MFA setup.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA secret and QR code generated"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MfaEnableResponse> enableMfa(@AuthenticationPrincipal UserDetails userDetails) {
        MfaEnableResponse response = authService.enableMfa(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/mfa/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify MFA code", description = "Verifies TOTP code and enables MFA for the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "MFA verified and enabled"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid MFA code"),
            @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> verifyMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerifyRequest request) {
        authService.verifyMfa(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/mfa/disable", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Disable MFA", description = "Disables MFA for the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "MFA disabled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> disableMfa(@AuthenticationPrincipal UserDetails userDetails) {
        authService.disableMfa(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}