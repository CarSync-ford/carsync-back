package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.ChurnDtos.ChurnPredictionResponse;
import br.com.sprint1.challenge.service.ChurnService;
import org.springframework.http.MediaType;
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
public class ChurnController {

    private final ChurnService churnService;

    public ChurnController(ChurnService churnService) {
        this.churnService = churnService;
    }

    @GetMapping("/customers/{customerId}")
    public ChurnPredictionResponse getPrediction(@PathVariable Long customerId) {
        return churnService.getPrediction(customerId);
    }

    @GetMapping("/risk-list")
    public List<ChurnPredictionResponse> getAllPredictions() {
        return churnService.getAllPredictions();
    }

    @PostMapping("/customers/{customerId}/recalculate")
    public ChurnPredictionResponse recalculate(@PathVariable Long customerId) {
        return churnService.getPrediction(customerId);
    }
}

