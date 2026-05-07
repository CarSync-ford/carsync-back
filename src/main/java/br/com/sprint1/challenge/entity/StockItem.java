package br.com.sprint1.challenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dealership_id", nullable = false)
    private Long dealershipId;

    @Column(name = "part_name", nullable = false)
    private String partName;

    @Column(name = "vehicle_family", nullable = false)
    private String vehicleFamily;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @Column(name = "forecast_demand", nullable = false)
    private Integer forecastDemand;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    public StockItem() {
    }

    public StockItem(Long id, Long dealershipId, String partName, String vehicleFamily, Integer quantityOnHand, Integer forecastDemand, String alertLevel) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.partName = partName;
        this.vehicleFamily = vehicleFamily;
        this.quantityOnHand = quantityOnHand;
        this.forecastDemand = forecastDemand;
        this.alertLevel = alertLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDealershipId() {
        return dealershipId;
    }

    public void setDealershipId(Long dealershipId) {
        this.dealershipId = dealershipId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getVehicleFamily() {
        return vehicleFamily;
    }

    public void setVehicleFamily(String vehicleFamily) {
        this.vehicleFamily = vehicleFamily;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Integer getForecastDemand() {
        return forecastDemand;
    }

    public void setForecastDemand(Integer forecastDemand) {
        this.forecastDemand = forecastDemand;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }
}

