package br.com.sprint1.challenge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record AuthRequest(
        @NotBlank @Email @Size(min = 5, max = 254) String email,
        @NotBlank @Size(min = 6, max = 20) String password
    ) {}

    public record AuthResponse(
        String token
    ) {}
}