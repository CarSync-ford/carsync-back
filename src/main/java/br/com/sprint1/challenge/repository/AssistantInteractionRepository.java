package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.AssistantInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantInteractionRepository extends JpaRepository<AssistantInteraction, Long> {

    List<AssistantInteraction> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
}

