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
        String token
    ) {}
}