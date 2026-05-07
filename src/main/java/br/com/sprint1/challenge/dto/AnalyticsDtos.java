package br.com.sprint1.challenge.dto;

import java.math.BigDecimal;
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
}

