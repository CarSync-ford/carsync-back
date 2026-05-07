package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.Customer360Dtos.Customer360Response;
import br.com.sprint1.challenge.service.Customer360Service;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/customers", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public class Customer360Controller {

    private final Customer360Service customer360Service;

    public Customer360Controller(Customer360Service customer360Service) {
        this.customer360Service = customer360Service;
    }

    @GetMapping("/{customerId}/360")
    public Customer360Response getCustomer360(@PathVariable Long customerId) {
        return customer360Service.getCustomer360(customerId);
    }
}

