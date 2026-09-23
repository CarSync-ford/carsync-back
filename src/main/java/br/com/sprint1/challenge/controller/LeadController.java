package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.LeadDtos.LeadConversionResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadUpdateRequest;
import br.com.sprint1.challenge.dto.LeadDtos.ProactiveLeadRequest;
import br.com.sprint1.challenge.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/leads", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@PreAuthorize("hasRole('USER')")
@Tag(name = "Leads", description = "Gestão de leads de pós-venda")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os leads ativos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão")
    })
    public List<LeadResponse> listAll() {
        return leadService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um lead por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lead encontrado"),
            @ApiResponse(responseCode = "404", description = "Lead não encontrado ou já removido")
    })
    public LeadResponse getById(@PathVariable Long id) {
        return leadService.getById(id);
    }

    @PostMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um lead proativo para um cliente/veículo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lead criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Cliente ou veículo não encontrado")
    })
    public LeadResponse create(@Valid @RequestBody ProactiveLeadRequest request) {
        return leadService.create(request);
    }

    @PutMapping(value = "/{id}", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @Operation(summary = "Atualiza título, descrição e urgência de um lead")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lead atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Lead não encontrado ou já removido")
    })
    public LeadResponse update(@PathVariable Long id, @Valid @RequestBody LeadUpdateRequest request) {
        return leadService.update(id, request);
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Converte um lead (marca como CONVERTED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lead convertido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lead não encontrado ou já removido")
    })
    public LeadConversionResponse convert(@PathVariable Long id) {
        return leadService.convert(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove (soft-delete) um lead")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lead removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lead não encontrado ou já removido")
    })
    public void delete(@PathVariable Long id) {
        leadService.delete(id);
    }
}

