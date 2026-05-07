package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantRequest;
import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantResponse;
import br.com.sprint1.challenge.entity.AssistantInteraction;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.repository.AssistantInteractionRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import br.com.sprint1.challenge.service.AssistantService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AssistantServiceImpl implements AssistantService {

    private final AssistantInteractionRepository assistantInteractionRepository;
    private final VehicleRepository vehicleRepository;

    public AssistantServiceImpl(AssistantInteractionRepository assistantInteractionRepository,
                                VehicleRepository vehicleRepository) {
        this.assistantInteractionRepository = assistantInteractionRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public VehicleAssistantResponse process(VehicleAssistantRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + request.vehicleId()));

        String input = request.inputText().toLowerCase(Locale.ROOT);
        String topic = detectTopic(input);
        String recommendation = recommendationFor(topic, vehicle.getModel());
        String urgency = urgencyFor(topic);

        AssistantInteraction interaction = new AssistantInteraction(
                null,
                request.vehicleId(),
                request.inputText(),
                topic,
                recommendation,
                LocalDateTime.now());

        AssistantInteraction saved = assistantInteractionRepository.save(interaction);

        return new VehicleAssistantResponse(
                saved.getId(),
                saved.getVehicleId(),
                saved.getDetectedTopic(),
                urgency,
                saved.getRecommendation(),
                saved.getCreatedAt());
    }

    @Override
    public List<VehicleAssistantResponse> history(Long vehicleId) {
        return assistantInteractionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(item -> new VehicleAssistantResponse(
                        item.getId(),
                        item.getVehicleId(),
                        item.getDetectedTopic(),
                        urgencyFor(item.getDetectedTopic()),
                        item.getRecommendation(),
                        item.getCreatedAt()))
                .toList();
    }

    private String detectTopic(String input) {
        if (input.contains("óleo")) {
            return "TROCA_DE_OLEO";
        }
        if (input.contains("bateria")) {
            return "BATERIA";
        }
        if (input.contains("ruído") || input.contains("barulho")) {
            return "RUÍDO";
        }
        if (input.contains("freio") || input.contains("pastilha")) {
            return "FREIOS";
        }
        return "SUPORTE_GERAL";
    }

    private String recommendationFor(String topic, String model) {
        return switch (topic) {
            case "TROCA_DE_OLEO" -> "Agendar troca de óleo para o " + model + " e verificar filtros.";
            case "BATERIA" -> "Recomendar teste de bateria e inspeção elétrica.";
            case "RUÍDO" -> "Orientar diagnóstico de suspensão e fixações.";
            case "FREIOS" -> "Recomendar inspeção do sistema de freios com prioridade.";
            default -> "Registrar atendimento e sugerir revisão preventiva do veículo.";
        };
    }

    private String urgencyFor(String topic) {
        return switch (topic) {
            case "FREIOS", "RUÍDO" -> "ALTA";
            case "BATERIA", "TROCA_DE_OLEO" -> "MÉDIA";
            default -> "BAIXA";
        };
    }
}

