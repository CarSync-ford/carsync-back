package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarrantyRiskRule implements ChurnRiskRule {

    private static final long DAYS_THRESHOLD = 180;

    @Override
    public RiskContribution evaluate(Vehicle vehicle, List<ServiceRecord> serviceHistory) {
        if (vehicle.isUnderWarrantyEndingWithin(DAYS_THRESHOLD)) {
            return RiskContribution.of(30, "Garantia perto do fim");
        }
        return RiskContribution.none();
    }
}
