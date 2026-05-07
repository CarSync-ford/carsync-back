package br.com.sprint1.challenge.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public final class LeadDtos {

    private LeadDtos() {
    }

    public record ProactiveLeadRequest(@NotNull Long customerId, Long vehicleId, String source) {
    }

    public record LeadResponse(Long id,
                               Long customerId,
                               Long vehicleId,
                               Long dealershipId,
                               String title,
                               String description,
                               String urgency,
                               String status,
                               String source,
                               LocalDateTime createdAt,
                               LocalDateTime convertedAt) {
    }

    public record LeadConversionResponse(Long leadId,
                                         String status,
                                         LocalDateTime convertedAt) {
    }
}

