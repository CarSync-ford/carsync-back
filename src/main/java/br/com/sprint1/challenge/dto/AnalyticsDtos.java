package br.com.sprint1.challenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record ServiceShareFilter(Long dealershipId, String vehicleModel, String serviceType) {
    }

    public record ServiceShareItem(String dealershipName,
                                   String vehicleModel,
                                   String serviceType,
                                   long serviceCount,
                                   BigDecimal sharePercentage) {
    }

    public record AnalyticsOverviewResponse(long totalCustomers,
                                             long totalVehicles,
                                             long totalLeads,
                                             long openLeads,
                                             List<ServiceShareItem> serviceShare) {
    }

    public record CustomerAnalyticsView(Long id,
                                        String fullName,
                                        String email,
                                        String phone,
                                        String city,
                                        String state,
                                        Long preferredDealershipId) {
    }

    public record LeadAnalyticsView(Long id,
                                    Long customerId,
                                    Long vehicleId,
                                    Long dealershipId,
                                    String title,
                                    String description,
                                    String urgency,
                                    String status,
                                    String source,
                                    LocalDateTime createdAt,
                                    LocalDateTime convertedAt) {
    }

    public record VehicleAnalyticsView(Long id,
                                       String model,
                                       String family,
                                       Integer modelYear,
                                       Integer mileage,
                                       Long dealershipId,
                                       LocalDate warrantyEndDate,
                                       String healthStatus) {
    }
}


