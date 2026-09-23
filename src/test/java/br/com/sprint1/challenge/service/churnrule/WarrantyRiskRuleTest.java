package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarrantyRiskRuleTest {

    private final WarrantyRiskRule rule = new WarrantyRiskRule();

    @Test
    void garantiaPertoDoFim_retorna30Pontos() {
        Vehicle vehicle = vehicleWithWarrantyEnd(LocalDate.now().plusDays(30));

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(30, result.points());
        assertEquals("Garantia perto do fim", result.reason().orElseThrow());
    }

    @Test
    void garantiaJaExpirada_retorna30Pontos() {
        Vehicle vehicle = vehicleWithWarrantyEnd(LocalDate.now().minusDays(10));

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(30, result.points());
    }

    @Test
    void garantiaLonge_naoContribui() {
        Vehicle vehicle = vehicleWithWarrantyEnd(LocalDate.now().plusYears(2));

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
        assertTrue(result.reason().isEmpty());
    }

    @Test
    void semDataDeGarantia_naoContribui() {
        Vehicle vehicle = vehicleWithWarrantyEnd(null);

        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
    }

    private Vehicle vehicleWithWarrantyEnd(LocalDate warrantyEndDate) {
        return new Vehicle(1L, "VIN123", "Corolla", "Sedan", 2023, 10000,
                1L, 1L, warrantyEndDate, "EXCELLENT");
    }
}
