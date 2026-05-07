package br.com.sprint1.challenge.service.impl;

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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChurnServiceImpl implements ChurnService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;

    public ChurnServiceImpl(CustomerRepository customerRepository,
                            VehicleRepository vehicleRepository,
                            ServiceRecordRepository serviceRecordRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
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
                riskLevel(score),
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
        int score = 20;
        Set<String> reasons = new LinkedHashSet<>();

        if (vehicle.getMileage() != null && vehicle.getMileage() > 40000) {
            score += 20;
            reasons.add("Quilometragem elevada");
        }

        if (vehicle.getWarrantyEndDate() != null) {
            long daysToWarrantyEnd = ChronoUnit.DAYS.between(LocalDate.now(), vehicle.getWarrantyEndDate());
            if (daysToWarrantyEnd <= 180) {
                score += 30;
                reasons.add("Garantia perto do fim");
            }
        }

        String healthStatus = vehicle.getHealthStatus() == null ? "" : vehicle.getHealthStatus().toUpperCase();
        if (healthStatus.contains("CRIT")) {
            score += 35;
            reasons.add("Saúde do veículo crítica");
        } else if (healthStatus.contains("WARN")) {
            score += 20;
            reasons.add("Saúde do veículo em alerta");
        }

        List<ServiceRecord> services = serviceRecordRepository.findByVehicleId(vehicle.getId());
        if (!services.isEmpty()) {
            LocalDate lastServiceDate = services.stream()
                    .map(ServiceRecord::getServiceDate)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            long daysSinceService = ChronoUnit.DAYS.between(lastServiceDate, LocalDate.now());
            if (daysSinceService > 180) {
                score += 15;
                reasons.add("Último serviço realizado há muito tempo");
            }
        }

        if (score > 100) {
            score = 100;
        }

        return new VehicleChurnInsight(vehicle.getId(), vehicle.getVin(), score, riskLevel(score), new ArrayList<>(reasons));
    }

    private String riskLevel(int score) {
        if (score >= 80) {
            return "ALTO";
        }
        if (score >= 50) {
            return "MÉDIO";
        }
        return "BAIXO";
    }
}

