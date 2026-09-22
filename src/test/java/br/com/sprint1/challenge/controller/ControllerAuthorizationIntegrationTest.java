package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.LeadStatus;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o controle de acesso por perfil (USER/ANALYST) adicionado em
 * ChurnController, Customer360Controller, LeadController, StockController e
 * AssistantController: endpoint publico x protegido e diferentes permissoes
 * por role, complementando {@link AnalyticsSecurityIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControllerAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DealershipRepository dealershipRepository;

    private String userToken;
    private String analystToken;
    private Long customerId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userToken = jwtService.generateToken("user-123", "user@test.com", "USER");
        analystToken = jwtService.generateToken("analyst-456", "analyst@test.com", "ANALYST");

        Dealership dealership = dealershipRepository.save(new Dealership(null, "Central Motors", "São Paulo", "SP"));

        Customer customer = customerRepository.save(new Customer(
                null, "Carlos Eduardo Santos", "carlos.santos@email.com", "11987654321", "São Paulo", "SP",
                dealership.getId()));
        customerId = customer.getId();

        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                null, "9BWZZZ377VT999999", "Corolla", "Sedan", 2023, 25000,
                customer.getId(), dealership.getId(), LocalDate.now().plusYears(2), "EXCELLENT"));

        leadRepository.save(new Lead(
                null, customer.getId(), vehicle.getId(), dealership.getId(),
                "Interesse em revisão preventiva", "Cliente deseja agendar revisão de 30.000 km",
                "MEDIUM", LeadStatus.OPEN, "PORTAL", LocalDateTime.now(), null));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        leadRepository.deleteAll();
        vehicleRepository.deleteAll();
        customerRepository.deleteAll();
        dealershipRepository.deleteAll();
    }

    // --- Churn: aberto para USER e ANALYST ---

    @Test
    @DisplayName("GET /api/v1/churn/risk-list sem token retorna 401")
    void churn_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/churn/risk-list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/churn/customers/{id} com ROLE_USER retorna 200")
    void churn_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/churn/customers/" + customerId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/churn/customers/{id} com ROLE_ANALYST retorna 200")
    void churn_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/churn/customers/" + customerId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());
    }

    // --- Customer 360: aberto para USER e ANALYST ---

    @Test
    @DisplayName("GET /api/v1/customers/{id}/360 sem token retorna 401")
    void customer360_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + customerId + "/360"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id}/360 com ROLE_USER retorna 200")
    void customer360_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + customerId + "/360")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id}/360 com ROLE_ANALYST retorna 200")
    void customer360_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + customerId + "/360")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());
    }

    // --- Leads: apenas USER (operacao do dia a dia) ---

    @Test
    @DisplayName("GET /api/v1/leads sem token retorna 401")
    void leads_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/leads com ROLE_USER retorna 200")
    void leads_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/leads com ROLE_ANALYST retorna 403")
    void leads_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());
    }

    // --- Stock: apenas USER ---

    @Test
    @DisplayName("GET /api/v1/stock/alerts sem token retorna 401")
    void stock_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stock/alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/stock/alerts com ROLE_USER retorna 200")
    void stock_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/stock/alerts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/stock/alerts com ROLE_ANALYST retorna 403")
    void stock_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/stock/alerts")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());
    }

    // --- Vehicle Assistant: apenas USER ---

    @Test
    @DisplayName("GET /api/v1/vehicle-assistant/interactions/{vehicleId} sem token retorna 401")
    void assistant_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-assistant/interactions/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/vehicle-assistant/interactions/{vehicleId} com ROLE_USER retorna 200")
    void assistant_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-assistant/interactions/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/vehicle-assistant/interactions/{vehicleId} com ROLE_ANALYST retorna 403")
    void assistant_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-assistant/interactions/1")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());
    }
}
