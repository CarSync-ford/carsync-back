package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.LeadDtos.LeadConversionResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadResponse;
import br.com.sprint1.challenge.dto.LeadDtos.LeadUpdateRequest;
import br.com.sprint1.challenge.dto.LeadDtos.ProactiveLeadRequest;
import br.com.sprint1.challenge.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/leads", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
@PreAuthorize("hasRole('USER')")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<LeadResponse> listAll() {
        return leadService.listAll();
    }

    @GetMapping("/{id}")
    public LeadResponse getById(@PathVariable Long id) {
        return leadService.getById(id);
    }

    @PostMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse create(@Valid @RequestBody ProactiveLeadRequest request) {
        return leadService.create(request);
    }

    @PutMapping(value = "/{id}", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    public LeadResponse update(@PathVariable Long id, @Valid @RequestBody LeadUpdateRequest request) {
        return leadService.update(id, request);
    }

    @PostMapping("/{id}/convert")
    public LeadConversionResponse convert(@PathVariable Long id) {
        return leadService.convert(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leadService.delete(id);
    }
}

