package br.com.sprint1.challenge.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public final class AssistantDtos {

    private AssistantDtos() {
    }

    public record VehicleAssistantRequest(@NotNull Long vehicleId, @NotNull String inputText) {
    }

    public record VehicleAssistantResponse(Long interactionId,
                                           Long vehicleId,
                                           String detectedTopic,
                                           String urgency,
                                           String recommendation,
                                           LocalDateTime createdAt) {
    }
}

