package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.StockDtos.StockAlertItem;
import br.com.sprint1.challenge.dto.StockDtos.StockPredictionResponse;
import br.com.sprint1.challenge.entity.StockItem;
import br.com.sprint1.challenge.repository.StockItemRepository;
import br.com.sprint1.challenge.service.StockService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final StockItemRepository stockItemRepository;

    public StockServiceImpl(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @Override
    public StockPredictionResponse getPredictions(Long dealershipId) {
        List<StockAlertItem> alerts = stockItemRepository.findAll().stream()
                .filter(item -> dealershipId == null || dealershipId.equals(item.getDealershipId()))
                .map(this::toAlert)
                .filter(alert -> alert.shortage() > 0)
                .sorted(Comparator.comparing(StockAlertItem::shortage).reversed())
                .toList();

        return new StockPredictionResponse(alerts);
    }

    private StockAlertItem toAlert(StockItem item) {
        int shortage = Math.max(item.getForecastDemand() - item.getQuantityOnHand(), 0);
        String alertLevel = shortage >= 15 ? "ALTA" : shortage >= 5 ? "MÉDIA" : "BAIXA";
        return new StockAlertItem(
                item.getId(),
                item.getDealershipId(),
                item.getPartName(),
                item.getVehicleFamily(),
                item.getQuantityOnHand(),
                item.getForecastDemand(),
                shortage,
                alertLevel);
    }
}

