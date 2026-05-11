package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        if (userTypeRepository.count() == 0) {
            userTypeRepository.save(new UserType("a1b2c3d4-e5f6-7890-abcd-ef1234567890", "USER"));
        }
    }

    @Test
    void postPayloadValido_retorna201ComId() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void postCpfDuplicado_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCpfInvalido_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password@1",
                "cpf", "12345678900"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postEmailInvalido_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "not-an-email",
                "password", "Password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSenhaCurta_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Pass@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSenhaSemMaiuscula_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSenhaSemEspecial_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCamposObrigatoriosAusentes_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarMfaEnabledFalseNoBanco() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        var savedUser = userRepository.findAll().get(0);
        assertFalse(savedUser.getMfaEnabled());
    }

    @Test
    void verificarSenhaHashedNoBanco() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "johndoe",
                "email", "john@example.com",
                "password", "Password@1",
                "cpf", "52998224725"
        );

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        var savedUser = userRepository.findAll().get(0);
        assertNotEquals("Password@1", savedUser.getHashedPassword());
        assertTrue(savedUser.getHashedPassword().startsWith("$2a$"));
    }
}