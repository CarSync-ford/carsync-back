package br.com.sprint1.challenge.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Value("${jwt.secret}")
    private String jwtSecret;

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

    @Test
    void refreshToken_withMalformedToken_returns401Not500() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "malformed.jwt.token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_withExpiredToken_returns401Not500() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("some-user")
                .claim("type", "REFRESH")
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", expiredToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedOperations_withValidBearerToken_injectsUserDetails() throws Exception {
        String password = "Test@1-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "auth-ops-user",
                                "email", email,
                                "password", password,
                                "cpf", "52998224725"))))
                .andExpect(status().isCreated());

        var login = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("token").asText();

        // enableMfa uses @AuthenticationPrincipal UserDetails userDetails
        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.qrCodeUri").isNotEmpty());

        // changePassword uses @AuthenticationPrincipal UserDetails userDetails
        String newPassword = "NewTest@1-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", password,
                                "newPassword", newPassword))))
                .andExpect(status().isNoContent());
    }
}
