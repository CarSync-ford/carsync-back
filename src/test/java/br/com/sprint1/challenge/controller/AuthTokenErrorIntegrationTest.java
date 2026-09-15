package br.com.sprint1.challenge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthTokenErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void refreshComJwtMalformadoRetorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"not-a-jwt\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetComJwtMalformadoRetorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content("{\"token\":\"not-a-jwt\",\"newPassword\":\"NewPassword1!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
