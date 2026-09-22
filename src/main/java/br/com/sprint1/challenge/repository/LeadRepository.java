package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByStatus(LeadStatus status);

    long countByStatus(LeadStatus status);

    List<Lead> findByCustomerId(Long customerId);

    List<Lead> findAllByDeletedAtBefore(java.time.LocalDateTime cutoff);
}

