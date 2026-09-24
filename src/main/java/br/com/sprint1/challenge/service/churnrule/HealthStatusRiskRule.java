package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HealthStatusRiskRule implements ChurnRiskRule {

    @Override
    public RiskContribution evaluate(Vehicle vehicle, List<ServiceRecord> serviceHistory) {
        if (vehicle.hasCriticalHealth()) {
            return RiskContribution.of(35, "Saúde do veículo crítica");
        }
        if (vehicle.hasWarningHealth()) {
            return RiskContribution.of(20, "Saúde do veículo em alerta");
        }
        return RiskContribution.none();
    }
}
