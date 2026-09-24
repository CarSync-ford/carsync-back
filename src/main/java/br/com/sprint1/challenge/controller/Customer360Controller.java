package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.Customer360Dtos.Customer360Response;
import br.com.sprint1.challenge.service.Customer360Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/customers", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@PreAuthorize("hasAnyRole('USER','ANALYST')")
@Tag(name = "Customer 360", description = "Visão consolidada do cliente (veículos, churn e leads abertos)")
@SecurityRequirement(name = "bearerAuth")
public class Customer360Controller {

    private final Customer360Service customer360Service;

    public Customer360Controller(Customer360Service customer360Service) {
        this.customer360Service = customer360Service;
    }

    @GetMapping("/{customerId}/360")
    @Operation(summary = "Retorna a visão 360 de um cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Visão 360 retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public Customer360Response getCustomer360(@PathVariable Long customerId) {
        return customer360Service.getCustomer360(customerId);
    }
}

