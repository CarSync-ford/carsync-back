package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "hmac.enabled=true",
    "hmac.secret=test-hmac-secret",
    "rate-limit.enabled=false"
})
@AutoConfigureMockMvc
class HmacSignatureFilterTest {

    private static final String SECRET = "test-hmac-secret";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectRequestWithoutSignatureHeader() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidSignature() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                .header("X-HMAC-Signature", "invalid-signature"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptGetRequestWithValidSignature() throws Exception {
        String payload = "/api/v1/leads";
        String signature = computeHmac(payload, SECRET);

        mockMvc.perform(get("/api/v1/leads")
                .header("X-HMAC-Signature", signature))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptGetRequestWithQueryStringAndValidSignature() throws Exception {
        String payload = "/api/v1/leads?page=1";
        String signature = computeHmac(payload, SECRET);

        mockMvc.perform(get("/api/v1/leads?page=1")
                .header("X-HMAC-Signature", signature))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptPostRequestWithBodyAndValidSignature() throws Exception {
        String body = "{\"name\":\"Test Lead\",\"email\":\"test@example.com\",\"phone\":\"11999999999\",\"vehicleInterest\":\"SUV\"}";
        String signature = computeHmac(body, SECRET);

        int status = mockMvc.perform(post("/api/v1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-HMAC-Signature", signature))
            .andReturn().getResponse().getStatus();

        assertNotEquals(401, status, "HMAC filter should not reject a valid signature");
    }

    @Test
    void shouldRejectPostRequestWithTamperedBody() throws Exception {
        String originalBody = "{\"name\":\"Test\"}";
        String signature = computeHmac(originalBody, SECRET);

        mockMvc.perform(post("/api/v1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Tampered\"}")
                .header("X-HMAC-Signature", signature))
            .andExpect(status().isUnauthorized());
    }

    private static String computeHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
    }
}
