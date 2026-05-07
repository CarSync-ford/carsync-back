package br.com.sprint1.challenge.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistant_interactions")
public class AssistantInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "input_text", nullable = false, length = 1000)
    private String inputText;

    @Column(name = "detected_topic", nullable = false)
    private String detectedTopic;

    @Column(nullable = false, length = 1000)
    private String recommendation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AssistantInteraction() {
    }

    public AssistantInteraction(Long id, Long vehicleId, String inputText, String detectedTopic, String recommendation, LocalDateTime createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.inputText = inputText;
        this.detectedTopic = detectedTopic;
        this.recommendation = recommendation;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getDetectedTopic() {
        return detectedTopic;
    }

    public void setDetectedTopic(String detectedTopic) {
        this.detectedTopic = detectedTopic;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
