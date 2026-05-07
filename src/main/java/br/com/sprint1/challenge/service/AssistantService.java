package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantRequest;
import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantResponse;

import java.util.List;

public interface AssistantService {

    VehicleAssistantResponse process(VehicleAssistantRequest request);

    List<VehicleAssistantResponse> history(Long vehicleId);
}

