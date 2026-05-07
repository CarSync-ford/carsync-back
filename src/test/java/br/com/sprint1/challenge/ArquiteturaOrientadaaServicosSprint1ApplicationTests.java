package br.com.sprint1.challenge;

import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.ServiceRecordRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArquiteturaOrientadaaServicosSprint1ApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DealershipRepository dealershipRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldExposeOpenApiDocs() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().contains("\"openapi\""));
    }

    @Test
    void shouldReturnRestCustomerRiskPrediction() {
        Long customerId = createCustomerForChurnScenario();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/churn/customers/" + customerId,
                String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().contains("\"customerId\":" + customerId));
        Assertions.assertTrue(response.getBody().contains("\"riskLevel\":"));
    }

    private Long createCustomerForChurnScenario() {
        long unique = System.nanoTime();

        Dealership dealership = dealershipRepository.save(new Dealership(
                null,
                "Ford Test",
                "Sao Paulo",
                "Sudeste"
        ));

        Customer customer = customerRepository.save(new Customer(
                null,
                "Cliente REST Teste",
                "rest-" + unique + "@email.com",
                "11999990000",
                "Sao Paulo",
                "SP",
                dealership.getId()
        ));

        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                null,
                "VIN" + unique,
                "T-Cross",
                "SUV",
                2022,
                45000,
                customer.getId(),
                dealership.getId(),
                LocalDate.now().plusMonths(3),
                "WARN"
        ));

        serviceRecordRepository.save(new ServiceRecord(
                null,
                vehicle.getId(),
                dealership.getId(),
                "Revisao",
                LocalDate.now().minusDays(220),
                new BigDecimal("500.00")
        ));

        return customer.getId();
    }
}

