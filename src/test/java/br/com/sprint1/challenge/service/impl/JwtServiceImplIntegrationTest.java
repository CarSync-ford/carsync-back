package br.com.sprint1.challenge.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.BeanCreationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplIntegrationTest {

    @Test
    void contextLoads_withValidJwtSecret() {
        String validSecret = "exactly32characterslongsecretkey!";
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .properties("app.cors.allowed-origins=https://app.example.com")
                .properties("jwt.secret=" + validSecret)
                .properties("jwt.expiration-minutes=30")
                .properties("jwt.issuer=carsync-auth")
                .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                .run()) {
            assertNotNull(context.getBean("jwtServiceImpl"));
        }
    }

    @Test
    void contextFails_withShortJwtSecret() {
        String shortSecret = "short";
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .properties("app.cors.allowed-origins=https://app.example.com")
                    .properties("jwt.secret=" + shortSecret)
                    .properties("jwt.expiration-minutes=30")
                    .properties("jwt.issuer=carsync-auth")
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run()) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("256 bits"));
    }

    @Test
    void contextFails_withEmptyJwtSecret() {
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .properties("app.cors.allowed-origins=https://app.example.com")
                    .properties("jwt.secret=")
                    .properties("jwt.expiration-minutes=30")
                    .properties("jwt.issuer=carsync-auth")
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run()) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("256 bits"));
    }

    @Test
    void contextFails_with31CharJwtSecret() {
        String shortSecret = "only31characterslongsecret!"; // 31 chars
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .properties("app.cors.allowed-origins=https://app.example.com")
                    .properties("jwt.secret=" + shortSecret)
                    .properties("jwt.expiration-minutes=30")
                    .properties("jwt.issuer=carsync-auth")
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run()) {
                context.close();
            }
        });
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertTrue(ex.getCause().getMessage().contains("256 bits"));
    }
}