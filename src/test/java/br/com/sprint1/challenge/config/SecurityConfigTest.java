package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpoint_withoutJwt_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk());
    }

    @Test
    void userMe_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeaders_presentOnHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Permissions-Policy", "geolocation=(), microphone=()"));
    }

    @Test
    void securityHeaders_presentOnActuatorProbes() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Permissions-Policy", "geolocation=(), microphone=()"));
    }

    @Test
    void securityHeaders_presentOnErrorResponses() throws Exception {
        mockMvc.perform(post("/api/v1/auth")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Permissions-Policy", "geolocation=(), microphone=()"));
    }

    @Test
    void authEndpoints_publicOnlyForSpecificPosts() throws Exception {
        // Public POST endpoints reach controller (bad request when body is empty, NOT 401)
        mockMvc.perform(post("/api/v1/auth")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void authEndpoints_protectedWithoutJwt_returns401() throws Exception {
        // Authenticated endpoints under /api/v1/auth/** return 401 when called without JWT
        mockMvc.perform(post("/api/v1/auth/change-password")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/mfa/enable"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/mfa/disable"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth"))
            .andExpect(status().isUnauthorized());
    }
}
