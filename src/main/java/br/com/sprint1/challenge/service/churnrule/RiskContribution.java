package br.com.sprint1.challenge.service.churnrule;

import java.util.Optional;

/**
 * Resultado de uma {@link ChurnRiskRule}: pontos a somar no score de churn e,
 * se aplicável, o motivo textual que explica a contribuição.
 */
public record RiskContribution(int points, Optional<String> reason) {

    public static RiskContribution none() {
        return new RiskContribution(0, Optional.empty());
    }

    public static RiskContribution of(int points, String reason) {
        return new RiskContribution(points, Optional.of(reason));
    }
}
