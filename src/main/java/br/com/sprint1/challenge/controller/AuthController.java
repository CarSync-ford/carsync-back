package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@Tag(name = "Authentication", description = "Authentication endpoints for login with pre-auth JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login with JSON payload", description = "Authenticates credentials and returns a pre-auth JWT token (30min). Next step: MFA verification (future feature).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful - returns JWT token"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> loginJson(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.token())
                .body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Login with form-urlencoded payload", description = "Authenticates credentials (form) and returns a pre-auth JWT token (30min). Next step: MFA verification (future feature).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful - returns JWT token"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> loginForm(@Valid @ModelAttribute AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.token())
                .body(response);
    }
}