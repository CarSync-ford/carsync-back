package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.ServiceRecordRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final LeadRepository leadRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final DealershipRepository dealershipRepository;

    public AnalyticsServiceImpl(CustomerRepository customerRepository,
                                VehicleRepository vehicleRepository,
                                LeadRepository leadRepository,
                                ServiceRecordRepository serviceRecordRepository,
                                DealershipRepository dealershipRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.leadRepository = leadRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.dealershipRepository = dealershipRepository;
    }

    @Override
    public AnalyticsOverviewResponse getOverview(Long dealershipId) {
        long totalCustomers = customerRepository.count();
        long totalVehicles = dealershipId == null ? vehicleRepository.count() : vehicleRepository.findAll().stream()
                .filter(vehicle -> Objects.equals(vehicle.getDealershipId(), dealershipId))
                .count();
        long totalLeads = dealershipId == null ? leadRepository.count() : leadRepository.findAll().stream()
                .filter(lead -> Objects.equals(lead.getDealershipId(), dealershipId))
                .count();
        long openLeads = dealershipId == null ? leadRepository.countByStatus("OPEN") : leadRepository.findAll().stream()
                .filter(lead -> Objects.equals(lead.getDealershipId(), dealershipId) && "OPEN".equalsIgnoreCase(lead.getStatus()))
                .count();

        return new AnalyticsOverviewResponse(totalCustomers, totalVehicles, totalLeads, openLeads, getServiceShare(dealershipId, null, null));
    }

    @Override
    public List<ServiceShareItem> getServiceShare(Long dealershipId, String vehicleModel, String serviceType) {
        List<ServiceRecord> records = serviceRecordRepository.findAll().stream()
                .filter(record -> dealershipId == null || Objects.equals(record.getDealershipId(), dealershipId))
                .filter(record -> serviceType == null || serviceType.isBlank() || record.getServiceType().equalsIgnoreCase(serviceType))
                .toList();

        if (records.isEmpty()) {
            return List.of();
        }

        Map<Long, Vehicle> vehicleById = vehicleRepository.findAll().stream()
                .collect(Collectors.toMap(Vehicle::getId, Function.identity()));
        Map<Long, Dealership> dealershipById = dealershipRepository.findAll().stream()
                .collect(Collectors.toMap(Dealership::getId, Function.identity()));

        List<ServiceRecord> filtered = new ArrayList<>();
        for (ServiceRecord record : records) {
            Vehicle vehicle = vehicleById.get(record.getVehicleId());
            if (vehicle == null) {
                continue;
            }
            if (vehicleModel != null && !vehicleModel.isBlank() && !vehicle.getModel().equalsIgnoreCase(vehicleModel)) {
                continue;
            }
            filtered.add(record);
        }

        long total = filtered.size();
        if (total == 0) {
            return List.of();
        }

        Map<String, Long> grouped = filtered.stream()
                .collect(Collectors.groupingBy(
                        record -> {
                            Vehicle vehicle = vehicleById.get(record.getVehicleId());
                            String dealershipName = dealershipById.getOrDefault(record.getDealershipId(), new Dealership(record.getDealershipId(), "Concessionária não identificada", "", "")).getName();
                            return dealershipName + "|" + vehicle.getModel() + "|" + record.getServiceType();
                        },
                        Collectors.counting()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    BigDecimal share = BigDecimal.valueOf(entry.getValue())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                    return new ServiceShareItem(parts[0], parts[1], parts[2], entry.getValue(), share);
                })
                .sorted(Comparator.comparing(ServiceShareItem::sharePercentage).reversed())
                .toList();
    }
}

