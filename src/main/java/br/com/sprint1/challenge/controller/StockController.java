package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.StockDtos.StockPredictionResponse;
import br.com.sprint1.challenge.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/stock", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@PreAuthorize("hasRole('USER')")
@Tag(name = "Stock", description = "Alertas de estoque de peças por concessionária")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Lista alertas de estoque (peças com previsão de falta)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas retornados com sucesso")
    })
    public StockPredictionResponse getAlerts(@RequestParam(required = false) Long dealershipId) {
        return stockService.getPredictions(dealershipId);
    }
}

