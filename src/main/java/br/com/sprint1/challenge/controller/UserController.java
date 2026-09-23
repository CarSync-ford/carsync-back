package br.com.sprint1.challenge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.sprint1.challenge.dto.UserDtos.CreateUserRequest;
import br.com.sprint1.challenge.dto.UserDtos.GetUserResponse;
import br.com.sprint1.challenge.dto.UserDtos.UserCreatedResponse;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/v1/user", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@Tag(name = "User", description = "Cadastro e dados do usuário autenticado")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuário (endpoint público)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado")
    })
    public UserCreatedResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Retorna os dados do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou usuário não encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GetUserResponse> me(Authentication authentication) {
        try {
            GetUserResponse response = userService.getById(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}