package br.com.sprint1.challenge.dto;

import java.util.List;

public final class StockDtos {

    private StockDtos() {
    }

    public record StockAlertItem(Long stockItemId,
                                 Long dealershipId,
                                 String partName,
                                 String vehicleFamily,
                                 int quantityOnHand,
                                 int forecastDemand,
                                 int shortage,
                                 String alertLevel) {
    }

    public record StockPredictionResponse(List<StockAlertItem> alerts) {
    }
}

