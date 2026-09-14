package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.CustomerAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.LeadAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;
import br.com.sprint1.challenge.dto.AnalyticsDtos.VehicleAnalyticsView;
import br.com.sprint1.challenge.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@PreAuthorize("hasRole('ANALYST')")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public AnalyticsOverviewResponse getOverview(@RequestParam(required = false) Long dealershipId,
                                                  Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "overview");
        return analyticsService.getOverview(dealershipId);
    }

    @GetMapping("/service-share")
    public List<ServiceShareItem> getServiceShare(@RequestParam(required = false) Long dealershipId,
                                                  @RequestParam(required = false) String vehicleModel,
                                                  @RequestParam(required = false) String serviceType,
                                                  Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "service-share");
        return analyticsService.getServiceShare(dealershipId, vehicleModel, serviceType);
    }

    @GetMapping("/customers")
    public List<CustomerAnalyticsView> getCustomers(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "customers");
        return analyticsService.getCustomersAnalytics();
    }

    @GetMapping("/leads")
    public List<LeadAnalyticsView> getLeads(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "leads");
        return analyticsService.getLeadsAnalytics();
    }

    @GetMapping("/vehicles")
    public List<VehicleAnalyticsView> getVehicles(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "vehicles");
        return analyticsService.getVehiclesAnalytics();
    }

    private String getUsername(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }
}
