package br.com.sprint1.challenge.dto;

import java.util.List;

public final class Customer360Dtos {

    private Customer360Dtos() {
    }

    public record CustomerVehicleResponse(Long vehicleId,
                                          String vin,
                                          String model,
                                          String family,
                                          Integer modelYear,
                                          Integer mileage,
                                          String healthStatus,
                                          String warrantyEndDate) {
    }

    public record Customer360Response(Long customerId,
                                      String customerName,
                                      String email,
                                      String phone,
                                      String city,
                                      String state,
                                      String preferredDealership,
                                      String churnRiskLevel,
                                      int churnScore,
                                      List<CustomerVehicleResponse> vehicles,
                                      List<LeadDtos.LeadResponse> openLeads) {
    }
}

