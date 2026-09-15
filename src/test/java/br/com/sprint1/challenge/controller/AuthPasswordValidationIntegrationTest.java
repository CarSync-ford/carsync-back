package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ResetPasswordRequest;
import br.com.sprint1.challenge.service.AuthService;
import br.com.sprint1.challenge.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void resetPassword_rejeitaSenhaFracaAntesDoServico() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "weakpass");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(request);
    }

    @Test
    void changePassword_rejeitaSenhaFracaAntesDoServico() throws Exception {
        String accessToken = "access-token";
        when(jwtService.parse(accessToken)).thenReturn(new io.jsonwebtoken.impl.DefaultClaims(
                java.util.Map.of("sub", "user-123", "role", "USER", "type", "ACCESS")));
        ChangePasswordRequest request = new ChangePasswordRequest("current-password", "weakpass");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).changePassword("user-123", request);
    }
}
