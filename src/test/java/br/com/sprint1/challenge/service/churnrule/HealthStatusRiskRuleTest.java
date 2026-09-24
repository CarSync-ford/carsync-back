package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthStatusRiskRuleTest {

    private final HealthStatusRiskRule rule = new HealthStatusRiskRule();

    @Test
    void saudeCritica_retorna35Pontos() {
        Vehicle vehicle = vehicleWithHealth("CRITICAL");

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(35, result.points());
        assertEquals("Saúde do veículo crítica", result.reason().orElseThrow());
    }

    @Test
    void saudeEmAlerta_retorna20Pontos() {
        Vehicle vehicle = vehicleWithHealth("WARNING");

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(20, result.points());
        assertEquals("Saúde do veículo em alerta", result.reason().orElseThrow());
    }

    @Test
    void saudeExcelente_naoContribui() {
        Vehicle vehicle = vehicleWithHealth("EXCELLENT");

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
        assertTrue(result.reason().isEmpty());
    }

    @Test
    void saudeNula_naoContribui() {
        Vehicle vehicle = vehicleWithHealth(null);

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
    }

    private Vehicle vehicleWithHealth(String healthStatus) {
        return new Vehicle(1L, "VIN123", "Corolla", "Sedan", 2023, 10000,
                1L, 1L, LocalDate.now().plusYears(1), healthStatus);
    }
}
