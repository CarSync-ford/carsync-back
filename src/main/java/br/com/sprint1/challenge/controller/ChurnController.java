package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.ChurnDtos.ChurnPredictionResponse;
import br.com.sprint1.challenge.service.ChurnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/churn", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@PreAuthorize("hasAnyRole('USER','ANALYST')")
@Tag(name = "Churn", description = "Predição de risco de churn por cliente")
@SecurityRequirement(name = "bearerAuth")
public class ChurnController {

    private final ChurnService churnService;

    public ChurnController(ChurnService churnService) {
        this.churnService = churnService;
    }

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Retorna a predição de churn de um cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Predição calculada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ChurnPredictionResponse getPrediction(@PathVariable Long customerId) {
        return churnService.getPrediction(customerId);
    }

    @GetMapping("/risk-list")
    @Operation(summary = "Lista a predição de churn de todos os clientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public List<ChurnPredictionResponse> getAllPredictions() {
        return churnService.getAllPredictions();
    }

    @PostMapping("/customers/{customerId}/recalculate")
    @Operation(summary = "Recalcula a predição de churn de um cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Predição recalculada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ChurnPredictionResponse recalculate(@PathVariable Long customerId) {
        return churnService.getPrediction(customerId);
    }
}

