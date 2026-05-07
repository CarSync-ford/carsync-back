package br.com.sprint1.challenge.repository;

import java.util.List;

import br.com.sprint1.challenge.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    List<StockItem> findByDealershipId(Long dealershipId);
}

