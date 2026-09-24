package br.com.sprint1.challenge.domain;

public enum RiskLevel {

    BAIXO("BAIXO"),
    MEDIO("MÉDIO"),
    ALTO("ALTO");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= 80) {
            return ALTO;
        }
        if (score >= 50) {
            return MEDIO;
        }
        return BAIXO;
    }
}
