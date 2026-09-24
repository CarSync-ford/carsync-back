package br.com.sprint1.challenge.service.churnrule;

import br.com.sprint1.challenge.entity.ServiceRecord;
import br.com.sprint1.challenge.entity.Vehicle;

import java.util.List;

/**
 * Strategy: cada implementação avalia um único fator de risco de churn.
 * {@code ChurnServiceImpl} soma as contribuições de todas as regras registradas
 * como beans Spring, sem precisar conhecer cada fator individualmente (OCP) —
 * uma nova regra é adicionada como uma nova classe, sem alterar as existentes.
 */
public interface ChurnRiskRule {

    RiskContribution evaluate(Vehicle vehicle, List<ServiceRecord> serviceHistory);
}
