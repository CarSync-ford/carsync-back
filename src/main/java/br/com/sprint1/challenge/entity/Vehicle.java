package br.com.sprint1.challenge.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String family;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Column(nullable = false)
    private Integer mileage;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "dealership_id", nullable = false)
    private Long dealershipId;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "health_status", nullable = false)
    private String healthStatus;

    public Vehicle() {
    }

    public Vehicle(Long id, String vin, String model, String family, Integer modelYear, Integer mileage, Long customerId, Long dealershipId, LocalDate warrantyEndDate, String healthStatus) {
        this.id = id;
        this.vin = vin;
        this.model = model;
        this.family = family;
        this.modelYear = modelYear;
        this.mileage = mileage;
        this.customerId = customerId;
        this.dealershipId = dealershipId;
        this.warrantyEndDate = warrantyEndDate;
        this.healthStatus = healthStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getDealershipId() {
        return dealershipId;
    }

    public void setDealershipId(Long dealershipId) {
        this.dealershipId = dealershipId;
    }

    public LocalDate getWarrantyEndDate() {
        return warrantyEndDate;
    }

    public void setWarrantyEndDate(LocalDate warrantyEndDate) {
        this.warrantyEndDate = warrantyEndDate;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }
}

