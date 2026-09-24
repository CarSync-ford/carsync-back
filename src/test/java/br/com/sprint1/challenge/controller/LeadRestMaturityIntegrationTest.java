package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.LeadStatus;
import br.com.sprint1.challenge.entity.UrgencyLevel;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre Maturidade REST nivel 2 no recurso Lead: PUT para atualizacao,
 * DELETE (soft-delete) e os status codes correspondentes (200/204/400/404).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeadRestMaturityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private DealershipRepository dealershipRepository;

    private String userToken;
    private Long leadId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userToken = jwtService.generateToken("user-123", "user@test.com", "USER");

        Dealership dealership = dealershipRepository.save(new Dealership(null, "Central Motors", "São Paulo", "SP"));

        Customer customer = customerRepository.save(new Customer(
                null, "Carlos Eduardo Santos", "carlos.santos@email.com", "11987654321", "São Paulo", "SP",
                dealership.getId()));

        Lead lead = leadRepository.save(new Lead(
                null, customer.getId(), null, dealership.getId(),
                "Título original", "Descrição original", UrgencyLevel.MEDIA, LeadStatus.OPEN, "PORTAL",
                LocalDateTime.now(), null));
        leadId = lead.getId();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        dealershipRepository.deleteAll();
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{id} com dados válidos retorna 200 e atualiza o recurso")
    void update_valido_retorna200() throws Exception {
        Map<String, String> payload = Map.of(
                "title", "Título atualizado",
                "description", "Descrição atualizada",
                "urgency", "ALTA"
        );

        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título atualizado"))
                .andExpect(jsonPath("$.urgency").value("ALTA"));
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{id} com título em branco retorna 400")
    void update_tituloEmBranco_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "title", "",
                "description", "Descrição atualizada",
                "urgency", "ALTA"
        );

        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{id} com urgency fora do enum retorna 400")
    void update_urgencyInvalida_retorna400() throws Exception {
        Map<String, String> payload = Map.of(
                "title", "Título atualizado",
                "description", "Descrição atualizada",
                "urgency", "URGENTISSIMO"
        );

        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{id} inexistente retorna 404")
    void update_inexistente_retorna404() throws Exception {
        Map<String, String> payload = Map.of(
                "title", "Título", "description", "Descrição", "urgency", "BAIXA"
        );

        mockMvc.perform(put("/api/v1/leads/999999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/leads/{id} retorna 204 e o recurso some da listagem")
    void delete_valido_retorna204EOculta() throws Exception {
        mockMvc.perform(delete("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/leads/{id} inexistente retorna 404")
    void delete_inexistente_retorna404() throws Exception {
        mockMvc.perform(delete("/api/v1/leads/999999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}
