package br.com.sprint1.challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:refresh-token-regression;DB_CLOSE_DELAY=-1",
        "jwt.refresh-token-expiry-days=30"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private final String email = UUID.randomUUID() + "@example.com";

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users_aud WHERE id IN (SELECT id FROM users WHERE email = ?)", email);
        jdbc.update("DELETE FROM users WHERE email = ?", email);
    }

    @Test
    void loginAndRefreshPersistCompleteTokens() throws Exception {
        String password = "Test@1-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "refresh-regression",
                                "email", email,
                                "password", password,
                                "cpf", "52998224725"))))
                .andExpect(status().isCreated());

        var login = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertTrue(refreshToken.length() > 255);
        assertTrue(refreshToken.equals(jdbc.queryForObject(
                "SELECT refresh_token FROM users WHERE email = ?", String.class, email)));
        assertNotNull(jdbc.queryForObject(
                "SELECT refresh_token_expires_at FROM users WHERE email = ?", java.sql.Timestamp.class, email));

        var refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedToken = objectMapper.readTree(refresh.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertTrue(rotatedToken.length() > 255);
        assertFalse(refreshToken.equals(rotatedToken));
        assertTrue(rotatedToken.equals(jdbc.queryForObject(
                "SELECT refresh_token FROM users WHERE email = ?", String.class, email)));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bothTokenColumnsHaveCapacityAndRemainNullable() {
        for (String table : new String[]{"USERS", "USERS_AUD"}) {
            var column = jdbc.queryForMap("""
                    SELECT CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = 'REFRESH_TOKEN'
                    """, table);
            assertEquals(1024L, ((Number) column.get("CHARACTER_MAXIMUM_LENGTH")).longValue(), table);
            assertEquals("YES", column.get("IS_NULLABLE"), table);
        }
    }
}
