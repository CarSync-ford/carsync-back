package br.com.sprint1.challenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Força HTTPS em todas as requisições
            .requiresChannel(channel -> 
                channel.anyRequest().requiresSecure()
            )
            // Suas outras configurações de CORS, CSRF, JWT, etc.
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}