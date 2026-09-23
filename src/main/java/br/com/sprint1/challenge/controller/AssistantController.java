package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantRequest;
import br.com.sprint1.challenge.dto.AssistantDtos.VehicleAssistantResponse;
import br.com.sprint1.challenge.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('USER')")
@Tag(name = "Vehicle Assistant", description = "Interações do assistente virtual do veículo")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "Registra uma interação do assistente virtual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Interação registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido")
    })
    public VehicleAssistantResponse process(@Valid @RequestBody VehicleAssistantRequest request) {
        return assistantService.process(request);
    }

    @GetMapping("/interactions/{vehicleId}")
    @Operation(summary = "Lista o histórico de interações de um veículo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    })
    public List<VehicleAssistantResponse> history(@PathVariable Long vehicleId) {
        return assistantService.history(vehicleId);
    }
}

