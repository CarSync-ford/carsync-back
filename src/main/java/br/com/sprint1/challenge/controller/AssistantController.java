package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantRequest;
import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantResponse;
import br.com.sprint1.challenge.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/vehicle-assistant", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(value = "/interactions", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleAssistantResponse process(@Valid @RequestBody VehicleAssistantRequest request) {
        return assistantService.process(request);
    }

    @GetMapping("/interactions/{vehicleId}")
    public List<VehicleAssistantResponse> history(@PathVariable Long vehicleId) {
        return assistantService.history(vehicleId);
    }
}

