package br.com.sprint1.challenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("FIAP Next - Ford Backend API")
                        .description("API REST da sprint de Arquitetura Orientada a Serviços e Web Services.")
                        .version("v1")
                        .contact(new Contact().name("Equipe Challenge FIAP Next"))
                        .license(new License().name("nosso")));
    }
}

