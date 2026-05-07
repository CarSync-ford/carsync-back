package br.com.sprint1.challenge.dto;

import java.util.List;

public final class ChurnDtos {

    private ChurnDtos() {
    }

    public record VehicleChurnInsight(Long vehicleId,
                                      String vin,
                                      int score,
                                      String riskLevel,
                                      List<String> reasons) {
    }

    public record ChurnPredictionResponse(Long customerId,
                                          String customerName,
                                          int score,
                                          String riskLevel,
                                          List<String> reasons,
                                          List<VehicleChurnInsight> vehicles) {
    }
}

