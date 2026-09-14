package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByDeletedAtBefore(LocalDateTime cutoff);
}

