package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.JwtService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsSecurityIntegrationTest {

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

    @BeforeEach
    void setUp() {
        userToken = jwtService.generateToken("user-123", "user@test.com", "USER");
        analystToken = jwtService.generateToken("analyst-456", "analyst@test.com", "ANALYST");

        Dealership dealership = dealershipRepository.save(new Dealership(null, "Central Motors", "São Paulo", "SP"));

        Customer customer = customerRepository.save(new Customer(
                null,
                "Carlos Eduardo Santos",
                "carlos.santos@email.com",
                "11987654321",
                "São Paulo",
                "SP",
                dealership.getId()
        ));

        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                null,
                "9BWZZZ377VT999999",
                "Corolla",
                "Sedan",
                2023,
                25000,
                customer.getId(),
                dealership.getId(),
                LocalDate.now().plusYears(2),
                "EXCELLENT"
        ));

        leadRepository.save(new Lead(
                null,
                customer.getId(),
                vehicle.getId(),
                dealership.getId(),
                "Interesse em revisão preventiva",
                "Cliente deseja agendar revisão de 30.000 km",
                "MEDIUM",
                "OPEN",
                "PORTAL",
                LocalDateTime.now(),
                null
        ));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/customers without token should return 401")
    void getCustomers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/customers with ROLE_USER should return 403")
    void getCustomers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/customers")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado."));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/customers with ROLE_ANALYST should return 200 with masked PII")
    void getCustomers_asAnalyst_returns200Masked() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/customers")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("C***** E****** S*****"))
                .andExpect(jsonPath("$[0].email").value("c***@email.com"))
                .andExpect(jsonPath("$[0].phone").value("(**) ****-****"))
                .andExpect(jsonPath("$[0].city").value("São Paulo"))
                .andExpect(jsonPath("$[0].state").value("SP"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/leads with ROLE_USER should return 403")
    void getLeads_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/leads")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/leads with ROLE_ANALYST should return 200")
    void getLeads_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/leads")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Interesse em revisão preventiva"))
                .andExpect(jsonPath("$[0].urgency").value("MEDIUM"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].source").value("PORTAL"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/vehicles with ROLE_USER should return 403")
    void getVehicles_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/vehicles")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/vehicles with ROLE_ANALYST should return 200 without customerId")
    void getVehicles_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/vehicles")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].model").value("Corolla"))
                .andExpect(jsonPath("$[0].family").value("Sedan"))
                .andExpect(jsonPath("$[0].modelYear").value(2023))
                .andExpect(jsonPath("$[0].mileage").value(25000))
                .andExpect(jsonPath("$[0].healthStatus").value("EXCELLENT"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/overview with ROLE_USER should return 403")
    void getOverview_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/overview with ROLE_ANALYST should return 200")
    void getOverview_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers", is(1)))
                .andExpect(jsonPath("$.totalVehicles", is(1)))
                .andExpect(jsonPath("$.totalLeads", is(1)))
                .andExpect(jsonPath("$.openLeads", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/service-share with ROLE_ANALYST should return 200")
    void getServiceShare_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/service-share")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());
    }
}
