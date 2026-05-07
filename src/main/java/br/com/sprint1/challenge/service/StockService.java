package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.StockDtos.StockPredictionResponse;

public interface StockService {

    StockPredictionResponse getPredictions(Long dealershipId);
}

