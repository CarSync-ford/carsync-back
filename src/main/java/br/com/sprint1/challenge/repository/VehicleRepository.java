package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByCustomerId(Long customerId);
}

