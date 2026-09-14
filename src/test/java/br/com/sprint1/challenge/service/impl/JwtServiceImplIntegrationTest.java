package br.com.sprint1.challenge.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.beans.factory.BeanCreationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplIntegrationTest {

    @Test
    void contextLoads_withValidJwtSecret() {
        String validSecret = "exactly32characterslongsecretkey!";
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .web(WebApplicationType.NONE)
                .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                .run("--app.cors.allowed-origins=https://app.example.com",
                     "--jwt.secret=" + validSecret,
                     "--jwt.expiration-minutes=30",
                     "--jwt.issuer=carsync-auth")) {
            assertNotNull(context.getBean("jwtServiceImpl"));
        }
    }

    @Test
    void contextFails_withShortJwtSecret() {
        String shortSecret = "short";
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .web(WebApplicationType.NONE)
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--app.cors.allowed-origins=https://app.example.com",
                         "--jwt.secret=" + shortSecret,
                         "--jwt.expiration-minutes=30",
                         "--jwt.issuer=carsync-auth")) {
                context.close();
            }
        });
        Throwable rootCause = NestedExceptionUtils.getRootCause(ex);
        assertTrue(rootCause instanceof IllegalStateException);
        assertTrue(rootCause.getMessage().contains("256 bits"));
    }

    @Test
    void contextFails_withEmptyJwtSecret() {
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .web(WebApplicationType.NONE)
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--app.cors.allowed-origins=https://app.example.com",
                         "--jwt.secret=",
                         "--jwt.expiration-minutes=30",
                         "--jwt.issuer=carsync-auth")) {
                context.close();
            }
        });
        Throwable rootCause = NestedExceptionUtils.getRootCause(ex);
        assertTrue(rootCause instanceof IllegalStateException);
        assertTrue(rootCause.getMessage().contains("256 bits"));
    }

    @Test
    void contextFails_with31CharJwtSecret() {
        String shortSecret = "only31characterslongsecret!1234"; // 31 chars
        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                    .web(WebApplicationType.NONE)
                    .sources(br.com.sprint1.challenge.ArquiteturaOrientadaaServicosSprint1Application.class)
                    .run("--app.cors.allowed-origins=https://app.example.com",
                         "--jwt.secret=" + shortSecret,
                         "--jwt.expiration-minutes=30",
                         "--jwt.issuer=carsync-auth")) {
                context.close();
            }
        });
        Throwable rootCause = NestedExceptionUtils.getRootCause(ex);
        assertTrue(rootCause instanceof IllegalStateException);
        assertTrue(rootCause.getMessage().contains("256 bits"));
    }
}