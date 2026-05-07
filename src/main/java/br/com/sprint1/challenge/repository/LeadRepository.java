package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByStatus(String status);

    long countByStatus(String status);

    List<Lead> findByCustomerId(Long customerId);
}

