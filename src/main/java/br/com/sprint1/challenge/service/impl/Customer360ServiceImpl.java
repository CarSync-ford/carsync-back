package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.ChurnDtos.ChurnPredictionResponse;
import br.com.sprint1.challenge.dto.Customer360Dtos.Customer360Response;
import br.com.sprint1.challenge.dto.Customer360Dtos.CustomerVehicleResponse;
import br.com.sprint1.challenge.dto.LeadDtos;
import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.ChurnService;
import br.com.sprint1.challenge.service.Customer360Service;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Customer360ServiceImpl implements Customer360Service {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final LeadRepository leadRepository;
    private final DealershipRepository dealershipRepository;
    private final ChurnService churnService;

    public Customer360ServiceImpl(CustomerRepository customerRepository,
                                  VehicleRepository vehicleRepository,
                                  LeadRepository leadRepository,
                                  DealershipRepository dealershipRepository,
                                  ChurnService churnService) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.leadRepository = leadRepository;
        this.dealershipRepository = dealershipRepository;
        this.churnService = churnService;
    }

    @Override
    public Customer360Response getCustomer360(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        List<CustomerVehicleResponse> vehicles = vehicleRepository.findByCustomerId(customerId).stream()
                .map(this::toVehicleResponse)
                .toList();

        ChurnPredictionResponse churn = churnService.getPrediction(customerId);

        String dealershipName = customer.getPreferredDealershipId() == null
                ? null
                : dealershipRepository.findById(customer.getPreferredDealershipId())
                .map(dealership -> dealership.getName())
                .orElse(null);

        List<LeadDtos.LeadResponse> openLeads = leadRepository.findByCustomerId(customerId).stream()
                .filter(lead -> "OPEN".equalsIgnoreCase(lead.getStatus()))
                .map(lead -> new LeadDtos.LeadResponse(
                        lead.getId(),
                        lead.getCustomerId(),
                        lead.getVehicleId(),
                        lead.getDealershipId(),
                        lead.getTitle(),
                        lead.getDescription(),
                        lead.getUrgency(),
                        lead.getStatus(),
                        lead.getSource(),
                        lead.getCreatedAt(),
                        lead.getConvertedAt()))
                .toList();

        return new Customer360Response(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCity(),
                customer.getState(),
                dealershipName,
                churn.riskLevel(),
                churn.score(),
                vehicles,
                openLeads);
    }

    private CustomerVehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new CustomerVehicleResponse(
                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getModel(),
                vehicle.getFamily(),
                vehicle.getModelYear(),
                vehicle.getMileage(),
                vehicle.getHealthStatus(),
                vehicle.getWarrantyEndDate() == null ? null : vehicle.getWarrantyEndDate().toString());
    }
}

