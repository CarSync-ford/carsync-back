package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.ChurnDtos.ChurnPredictionResponse;

import java.util.List;

public interface ChurnService {

    ChurnPredictionResponse getPrediction(Long customerId);

    List<ChurnPredictionResponse> getAllPredictions();
}

