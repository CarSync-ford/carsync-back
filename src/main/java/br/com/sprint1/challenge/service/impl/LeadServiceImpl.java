package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.LeadDtos.LeadConversionResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadResponse;
import br.com.sprint1.challenge.dto.LeadDtos.ProactiveLeadRequest;
import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.LeadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final DealershipRepository dealershipRepository;

    public LeadServiceImpl(LeadRepository leadRepository,
                           CustomerRepository customerRepository,
                           VehicleRepository vehicleRepository,
                           DealershipRepository dealershipRepository) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.dealershipRepository = dealershipRepository;
    }

    @Override
    public List<LeadResponse> listAll() {
        return leadRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public LeadResponse getById(Long id) {
        return toResponse(findLead(id));
    }

    @Transactional
    @Override
    public LeadResponse create(ProactiveLeadRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.customerId()));

        Vehicle vehicle = null;
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + request.vehicleId()));
        }

        Long dealershipId = vehicle != null ? vehicle.getDealershipId() : customer.getPreferredDealershipId();
        if (dealershipId == null) {
            dealershipId = dealershipRepository.findAll().stream()
                    .findFirst()
                    .map(dealership -> dealership.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nenhuma concessionária disponível para gerar o lead."));
        }

        String title = vehicle == null ? "Lead proativo de pós-venda" : "Lead proativo para " + vehicle.getModel();
        String description = vehicle == null
                ? "Recomendação baseada em histórico do cliente e sinais de retenção."
                : "O veículo " + vehicle.getVin() + " apresenta oportunidade de contato proativo.";
        String urgency = determineUrgency(vehicle);

        Lead lead = new Lead(null,
                customer.getId(),
                vehicle == null ? null : vehicle.getId(),
                dealershipId,
                title,
                description,
                urgency,
                "OPEN",
                request.source() == null || request.source().isBlank() ? "NEXT_BEST_ACTION" : request.source(),
                LocalDateTime.now(),
                null);

        return toResponse(leadRepository.save(lead));
    }

    @Transactional
    @Override
    public LeadConversionResponse convert(Long id) {
        Lead lead = findLead(id);
        lead.setStatus("CONVERTED");
        lead.setConvertedAt(LocalDateTime.now());
        leadRepository.save(lead);
        return new LeadConversionResponse(lead.getId(), lead.getStatus(), lead.getConvertedAt());
    }

    private Lead findLead(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado: " + id));
    }

    private String determineUrgency(Vehicle vehicle) {
        if (vehicle == null) {
            return "MÉDIA";
        }
        String health = vehicle.getHealthStatus() == null ? "" : vehicle.getHealthStatus().toUpperCase();
        if (health.contains("CRIT")) {
            return "ALTA";
        }
        if (health.contains("WARN")) {
            return "MÉDIA";
        }
        return "BAIXA";
    }

    private LeadResponse toResponse(Lead lead) {
        return new LeadResponse(
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
                lead.getConvertedAt());
    }
}

