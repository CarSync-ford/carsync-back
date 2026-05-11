package br.com.sprint1.challenge.dto;

import br.com.sprint1.challenge.validation.StrongPassword;
import br.com.sprint1.challenge.validation.ValidCpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class UserDtos {
    private UserDtos() {}

    public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @StrongPassword String password,
        @NotBlank @ValidCpf String cpf
    ) {}

    public record UserCreatedResponse(
        String id
    ) {}
}