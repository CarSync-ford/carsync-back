package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.StockDtos.StockPredictionResponse;
import br.com.sprint1.challenge.service.StockService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/stock", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/alerts")
    public StockPredictionResponse getAlerts(@RequestParam(required = false) Long dealershipId) {
        return stockService.getPredictions(dealershipId);
    }
}

