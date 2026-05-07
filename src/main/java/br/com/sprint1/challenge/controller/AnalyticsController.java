package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;
import br.com.sprint1.challenge.service.AnalyticsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/analytics", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public AnalyticsOverviewResponse getOverview(@RequestParam(required = false) Long dealershipId) {
        return analyticsService.getOverview(dealershipId);
    }

    @GetMapping("/service-share")
    public List<ServiceShareItem> getServiceShare(@RequestParam(required = false) Long dealershipId,
                                                  @RequestParam(required = false) String vehicleModel,
                                                  @RequestParam(required = false) String serviceType) {
        return analyticsService.getServiceShare(dealershipId, vehicleModel, serviceType);
    }
}

