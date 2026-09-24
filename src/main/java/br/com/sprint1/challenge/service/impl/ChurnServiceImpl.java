package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.domain.RiskLevel;
import br.com.sprint1.challenge.dto.ChurnDtos.ChurnPredictionResponse;
import br.com.sprint1.challenge.dto.ChurnDtos.VehicleChurnInsight;
import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.ServiceRecordRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.ChurnService;
import br.com.sprint1.challenge.service.churnrule.ChurnRiskRule;
import br.com.sprint1.challenge.service.churnrule.RiskContribution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Orquestra o cálculo de churn combinando as {@link ChurnRiskRule} registradas
 * como beans (Strategy pattern) — depende só da abstração, não das regras
 * concretas (DIP), e novas regras podem ser adicionadas sem alterar esta classe (OCP).
 */
@Service
public class ChurnServiceImpl implements ChurnService {

    private static final int BASE_SCORE = 20;
    private static final int MAX_SCORE = 100;

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final List<ChurnRiskRule> riskRules;

    public ChurnServiceImpl(CustomerRepository customerRepository,
                            VehicleRepository vehicleRepository,
                            ServiceRecordRepository serviceRecordRepository,
                            List<ChurnRiskRule> riskRules) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.riskRules = riskRules;
    }

    @Override
    public ChurnPredictionResponse getPrediction(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        List<VehicleChurnInsight> vehicleInsights = vehicleRepository.findByCustomerId(customerId).stream()
                .map(this::buildVehicleInsight)
                .toList();

        int score = vehicleInsights.stream().mapToInt(VehicleChurnInsight::score).max().orElse(0);
        Set<String> reasons = new LinkedHashSet<>();
        vehicleInsights.forEach(insight -> reasons.addAll(insight.reasons()));

        return new ChurnPredictionResponse(
                customer.getId(),
                customer.getFullName(),
                score,
                RiskLevel.fromScore(score).label(),
                new ArrayList<>(reasons),
                vehicleInsights);
    }

    @Override
    public List<ChurnPredictionResponse> getAllPredictions() {
        return customerRepository.findAll().stream()
                .map(customer -> getPrediction(customer.getId()))
                .toList();
    }

    private VehicleChurnInsight buildVehicleInsight(Vehicle vehicle) {
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleId(vehicle.getId());

        int score = BASE_SCORE;
        Set<String> reasons = new LinkedHashSet<>();
        for (ChurnRiskRule rule : riskRules) {
            RiskContribution contribution = rule.evaluate(vehicle, services);
            score += contribution.points();
            contribution.reason().ifPresent(reasons::add);
        }

        if (score > MAX_SCORE) {
            score = MAX_SCORE;
        }

        return new VehicleChurnInsight(vehicle.getId(), vehicle.getVin(), score,
                RiskLevel.fromScore(score).label(), new ArrayList<>(reasons));
    }
}

