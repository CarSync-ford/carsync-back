package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}

