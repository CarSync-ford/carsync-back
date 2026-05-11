package br.com.sprint1.challenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ArquiteturaOrientadaaServicosSprint1Application {

    private static final Logger log = LoggerFactory.getLogger(ArquiteturaOrientadaaServicosSprint1Application.class);

    public static void main(String[] args) {
        SpringApplication.run(ArquiteturaOrientadaaServicosSprint1Application.class, args);
    }

    @Bean
    public CommandLineRunner swaggerLinks() {
        return args -> {
            log.info("=".repeat(60));
            log.info("API Documentation available at:");
            log.info("  Swagger UI: http://localhost:8080/swagger-ui.html");
            log.info("  OpenAPI JSON: http://localhost:8080/v3/api-docs");
            log.info("=".repeat(60));
        };
    }
}

