package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.LeadDtos.LeadConversionResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadUpdateRequest;
import br.com.sprint1.challenge.dto.LeadDtos.ProactiveLeadRequest;

import java.util.List;

public interface LeadService {

    List<LeadResponse> listAll();

    LeadResponse getById(Long id);

    LeadResponse create(ProactiveLeadRequest request);

    LeadResponse update(Long id, LeadUpdateRequest request);

    LeadConversionResponse convert(Long id);

    void delete(Long id);
}

