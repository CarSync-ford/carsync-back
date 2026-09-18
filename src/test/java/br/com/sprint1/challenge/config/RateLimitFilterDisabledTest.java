package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "rate-limit.enabled=false")
@AutoConfigureMockMvc
class RateLimitFilterDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAllRequestsWhenRateLimitingDisabledForApimOffload() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/v1/health").secure(true)
                    .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk());
        }
    }
}
