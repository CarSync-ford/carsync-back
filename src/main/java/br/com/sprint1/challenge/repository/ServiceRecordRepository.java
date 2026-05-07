package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    List<ServiceRecord> findByVehicleId(Long vehicleId);

    List<ServiceRecord> findByDealershipId(Long dealershipId);
}

