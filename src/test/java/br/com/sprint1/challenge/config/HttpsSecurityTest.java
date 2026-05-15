package br.com.sprint1.challenge.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para verificar que o sistema aceita apenas requisições HTTPS.
 * <p>
 * A configuração {@code requiresChannel().anyRequest().requiresSecure()} no
 * {@link SecurityConfig} deve redirecionar qualquer requisição HTTP para HTTPS.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HttpsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Requisição HTTP deve ser redirecionada para HTTPS (302)")
    void httpRequest_shouldRedirectToHttps() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("https://**"));
    }

    @Test
    @DisplayName("Requisição HTTPS não deve ser redirecionada")
    void httpsRequest_shouldNotRedirect() throws Exception {
        // .secure(true) simula uma requisição HTTPS no MockMvc
        // Espera 401 (não autenticado) em vez de 3xx (redirect)
        mockMvc.perform(get("/api/users").secure(true))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status < 300 || status >= 400 :
                    "Esperava status fora da faixa 3xx, mas recebeu " + status;
            });
    }

    @Test
    @DisplayName("Requisição HTTP em qualquer endpoint deve redirecionar para HTTPS")
    void httpRequest_anyEndpoint_shouldRedirectToHttps() throws Exception {
        mockMvc.perform(get("/any-random-path"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location",
                org.hamcrest.Matchers.startsWith("https://")));
    }

    @Test
    @DisplayName("Requisição HTTPS deve passar pelo channel security (não redireciona)")
    void httpsRequest_shouldPassChannelSecurity() throws Exception {
        // Mesmo sem autenticação, o channel security não deve barrar.
        // O status esperado é 401/403 (por falta de auth), mas NÃO 3xx.
        mockMvc.perform(get("/api/users").secure(true))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status < 300 || status >= 400 :
                    "Esperava status fora da faixa 3xx, mas recebeu " + status;
            });
    }
}
