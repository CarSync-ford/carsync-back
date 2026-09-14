package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.CustomerAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.LeadAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;
import br.com.sprint1.challenge.dto.AnalyticsDtos.VehicleAnalyticsView;

import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverview(Long dealershipId);

    List<ServiceShareItem> getServiceShare(Long dealershipId, String vehicleModel, String serviceType);

    List<CustomerAnalyticsView> getCustomersAnalytics();

    List<LeadAnalyticsView> getLeadsAnalytics();

    List<VehicleAnalyticsView> getVehiclesAnalytics();
}

