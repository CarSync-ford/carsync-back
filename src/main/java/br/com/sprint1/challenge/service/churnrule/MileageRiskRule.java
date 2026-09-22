package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MileageRiskRule implements ChurnRiskRule {

    @Override
    public RiskContribution evaluate(Vehicle vehicle, List<ServiceRecord> serviceHistory) {
        if (vehicle.isHighMileage()) {
            return RiskContribution.of(20, "Quilometragem elevada");
        }
        return RiskContribution.none();
    }
}
