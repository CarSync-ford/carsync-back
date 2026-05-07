package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealershipRepository extends JpaRepository<Dealership, Long> {
}

