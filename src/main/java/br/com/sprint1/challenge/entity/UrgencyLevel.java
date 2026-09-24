package br.com.sprint1.challenge.entity;

public enum UrgencyLevel {

    BAIXA("BAIXA"),
    MEDIA("MÉDIA"),
    ALTA("ALTA");

    private final String label;

    UrgencyLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
