package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceHistoryRiskRuleTest {

    private final ServiceHistoryRiskRule rule = new ServiceHistoryRiskRule();
    private final Vehicle vehicle = new Vehicle(1L, "VIN123", "Corolla", "Sedan", 2023, 10000,
            1L, 1L, LocalDate.now().plusYears(1), "EXCELLENT");

    @Test
    void semHistoricoDeServico_naoContribui() {
        RiskContribution result = rule.evaluate(vehicle, List.of());

        assertEquals(0, result.points());
        assertTrue(result.reason().isEmpty());
    }

    @Test
    void ultimoServicoRecente_naoContribui() {
        List<ServiceRecord> history = List.of(serviceRecordOn(LocalDate.now().minusDays(30)));

        RiskContribution result = rule.evaluate(vehicle, history);

        assertEquals(0, result.points());
    }

    @Test
    void ultimoServicoAntigo_retorna15Pontos() {
        List<ServiceRecord> history = List.of(serviceRecordOn(LocalDate.now().minusDays(200)));

        RiskContribution result = rule.evaluate(vehicle, history);

        assertEquals(15, result.points());
        assertEquals("Último serviço realizado há muito tempo", result.reason().orElseThrow());
    }

    @Test
    void consideraApenasOServicoMaisRecente() {
        List<ServiceRecord> history = List.of(
                serviceRecordOn(LocalDate.now().minusDays(300)),
                serviceRecordOn(LocalDate.now().minusDays(10)));

        RiskContribution result = rule.evaluate(vehicle, history);

        assertEquals(0, result.points());
    }

    private ServiceRecord serviceRecordOn(LocalDate date) {
        return new ServiceRecord(1L, vehicle.getId(), 1L, "Revisão", date, new BigDecimal("300.00"));
    }
}
