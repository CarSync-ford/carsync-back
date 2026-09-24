package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ServiceHistoryRiskRule implements ChurnRiskRule {

    private static final long DAYS_THRESHOLD = 180;

    @Override
    public RiskContribution evaluate(Vehicle vehicle, List<ServiceRecord> serviceHistory) {
        if (serviceHistory.isEmpty()) {
            return RiskContribution.none();
        }

        LocalDate lastServiceDate = serviceHistory.stream()
                .map(ServiceRecord::getServiceDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        long daysSinceService = ChronoUnit.DAYS.between(lastServiceDate, LocalDate.now());
        if (daysSinceService > DAYS_THRESHOLD) {
            return RiskContribution.of(15, "Último serviço realizado há muito tempo");
        }
        return RiskContribution.none();
    }
}
