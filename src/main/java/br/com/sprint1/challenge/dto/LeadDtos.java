package br.com.sprint1.challenge.dto;

import java.time.LocalDateTime;

import br.com.sprint1.challenge.entity.UrgencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class LeadDtos {

    private LeadDtos() {
    }

    public record ProactiveLeadRequest(@NotNull Long customerId, Long vehicleId, String source) {
    }

    public record LeadUpdateRequest(@NotBlank String title,
                                    @NotBlank String description,
                                    @NotNull UrgencyLevel urgency) {
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

