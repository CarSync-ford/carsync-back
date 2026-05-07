package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;

import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverview(Long dealershipId);

    List<ServiceShareItem> getServiceShare(Long dealershipId, String vehicleModel, String serviceType);
}

