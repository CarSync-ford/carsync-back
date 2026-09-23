package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MileageRiskRuleTest {

    private final MileageRiskRule rule = new MileageRiskRule();

    @Test
    void quilometragemAlta_retorna20Pontos() {
        Vehicle vehicle = vehicleWithMileage(45000);

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(20, result.points());
        assertTrue(result.reason().isPresent());
        assertEquals("Quilometragem elevada", result.reason().get());
    }

    @Test
    void quilometragemBaixa_naoContribui() {
        Vehicle vehicle = vehicleWithMileage(10000);

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
        assertTrue(result.reason().isEmpty());
    }

    @Test
    void quilometragemNula_naoContribui() {
        Vehicle vehicle = vehicleWithMileage(null);

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
    }

    private Vehicle vehicleWithMileage(Integer mileage) {
        return new Vehicle(1L, "VIN123", "Corolla", "Sedan", 2023, mileage,
                1L, 1L, LocalDate.now().plusYears(1), "EXCELLENT");
    }
}
