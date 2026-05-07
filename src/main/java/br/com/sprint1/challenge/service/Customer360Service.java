package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.Customer360Dtos.Customer360Response;

public interface Customer360Service {

    Customer360Response getCustomer360(Long customerId);
}

