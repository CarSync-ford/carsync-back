package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.AnalyticsDtos.AnalyticsOverviewResponse;
import br.com.sprint1.challenge.dto.AnalyticsDtos.CustomerAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.LeadAnalyticsView;
import br.com.sprint1.challenge.dto.AnalyticsDtos.ServiceShareItem;
import br.com.sprint1.challenge.dto.AnalyticsDtos.VehicleAnalyticsView;
import br.com.sprint1.challenge.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Analytics", description = "Indicadores agregados de negócio (somente ANALYST)")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Retorna a visão geral de indicadores (clientes, veículos, leads)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicadores retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige ANALYST)")
    })
    public AnalyticsOverviewResponse getOverview(@RequestParam(required = false) Long dealershipId,
                                                  Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "overview");
        return analyticsService.getOverview(dealershipId);
    }

    @GetMapping("/service-share")
    @Operation(summary = "Retorna o percentual de serviços por concessionária/modelo/tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Percentuais retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige ANALYST)")
    })
    public List<ServiceShareItem> getServiceShare(@RequestParam(required = false) Long dealershipId,
                                                  @RequestParam(required = false) String vehicleModel,
                                                  @RequestParam(required = false) String serviceType,
                                                  Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "service-share");
        return analyticsService.getServiceShare(dealershipId, vehicleModel, serviceType);
    }

    @GetMapping("/customers")
    @Operation(summary = "Lista clientes com dados sensíveis mascarados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige ANALYST)")
    })
    public List<CustomerAnalyticsView> getCustomers(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "customers");
        return analyticsService.getCustomersAnalytics();
    }

    @GetMapping("/leads")
    @Operation(summary = "Lista todos os leads para fins analíticos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige ANALYST)")
    })
    public List<LeadAnalyticsView> getLeads(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "leads");
        return analyticsService.getLeadsAnalytics();
    }

    @GetMapping("/vehicles")
    @Operation(summary = "Lista todos os veículos para fins analíticos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (exige ANALYST)")
    })
    public List<VehicleAnalyticsView> getVehicles(Authentication authentication) {
        log.info("ANALYTICS_ACCESS user:{} resource:{}", getUsername(authentication), "vehicles");
        return analyticsService.getVehiclesAnalytics();
    }

    private String getUsername(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }
}
