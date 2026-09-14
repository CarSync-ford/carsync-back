package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.BeanCreationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsIntegrationTest {

    @Test
    void contextLoads_withValidCorsConfig() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                .run("--server.port=0", "--app.cors.allowed-origins=https://app.example.com,https://admin.example.com")) {
            assertNotNull(context.getBean("securityConfig"));
        }
    }

    @Test
    void contextLoads_withSingleValidCorsConfig() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                .run("--server.port=0", "--app.cors.allowed-origins=https://app.example.com")) {
            assertNotNull(context.getBean("securityConfig"));
        }
    }

    @Test
    void contextFails_withBlankCorsConfig() {
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--server.port=0", "--app.cors.allowed-origins=")) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("CORS_ALLOWED_ORIGINS must be set"));
    }

    @Test
    void contextFails_withWildcardCorsConfig() {
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--server.port=0", "--app.cors.allowed-origins=*")) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("CORS_ALLOWED_ORIGINS must be set"));
    }

    @Test
    void contextFails_withWhitespaceCorsConfig() {
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--server.port=0", "--app.cors.allowed-origins=   ")) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("CORS_ALLOWED_ORIGINS must be set"));
    }
}