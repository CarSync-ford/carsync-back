package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SecurityConfigCorsTest {

    @Test
    void validateCors_validOrigins_doesNotThrow() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example.com,https://admin.example.com");
        assertDoesNotThrow(config::validateCors);
    }

    @Test
    void validateCors_singleValidOrigin_doesNotThrow() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example.com");
        assertDoesNotThrow(config::validateCors);
    }

    @Test
    void validateCors_blank_throwsIllegalStateException() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "");
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateCors);
        assert ex.getMessage().contains("CORS_ALLOWED_ORIGINS must be set");
    }

    @Test
    void validateCors_null_throwsIllegalStateException() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", (String) null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateCors);
        assert ex.getMessage().contains("CORS_ALLOWED_ORIGINS must be set");
    }

    @Test
    void validateCors_wildcard_throwsIllegalStateException() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "*");
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateCors);
        assert ex.getMessage().contains("CORS_ALLOWED_ORIGINS must be set");
    }

    @Test
    void validateCors_whitespace_throwsIllegalStateException() {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "   ");
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateCors);
        assert ex.getMessage().contains("CORS_ALLOWED_ORIGINS must be set");
    }
}