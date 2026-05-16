package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "rate-limit.enabled=true")
@AutoConfigureMockMvc
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/health").secure(true)
                    .header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk());
        }
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/health").secure(true)
                .header("X-Forwarded-For", "10.0.0.2"));
        }

        mockMvc.perform(get("/api/v1/health").secure(true)
                .header("X-Forwarded-For", "10.0.0.2"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldHaveIndependentBucketsPerIp() throws Exception {
        // Exhaust limit for IP 1.1.1.1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/health").secure(true)
                .header("X-Forwarded-For", "1.1.1.1"));
        }

        // Different IP should still be allowed
        mockMvc.perform(get("/api/v1/health").secure(true)
                .header("X-Forwarded-For", "2.2.2.2"))
            .andExpect(status().isOk());
    }
}
