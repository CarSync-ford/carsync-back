# Phase 5 — Compliance e entrega consolidada

**Estado:** plano revisado; não pressupõe controles ou conformidade implementados.
**Origem:** `SEC-REQUIREMENTS.md:2,54–68` (RISK, OWASP, LGPD, CONT, FINAL).
**Contrato:** `.specs/README.md`.

## FINAL-1 — Documento único por atividade [Low]

**Arquivo-alvo:** `docs/security/SEC-DELIVERY.md`.

Criar ou consolidar, sem duplicar documentação existente, exatamente estas quatro seções principais:

1. **Pipeline DevSecOps Integrado — peso 3,0:** diagrama, etapas, riscos e execução Ford (phase-3).
2. **Segurança em Código e Infraestrutura — peso 2,5:** criptografia local, rate limit/validação/JWT, três perfis, MQTT/TLS, IaC aplicável; código, prints, commits e explicações (phase-0, phase-1, phase-2, phase-8).
3. **Observabilidade, Monitoramento e Resposta — peso 2,0:** plano, logs, métricas/alertas API/mobile/IoT/ML, prints de dashboards e resposta (phase-4).
4. **Compliance, Riscos e Segurança Contínua — peso 2,5:** revisão de riscos, mapeamentos, rotinas e checklist final (tarefas abaixo).

Referenciar anexos/artefatos sem exigir que avaliador reconstrua entrega a partir de documentos soltos. Cada evidência inclui origem, caminho/link, data/versão e resultado; commits somente quando já existentes ou autorizados, nunca criados automaticamente para cumprir checklist.

**Aceite:** quatro atividades no mesmo documento, cobrindo arquitetura, API, mobile, IoT, dados e ML. Pendências externas continuam identificadas, não mascaradas por N/A genérico.

## RISK-1 — STRIDE + DevSecOps [High]

**Entradas:** arquitetura real, implementação e evidências das outras specs.
**Arquivo-alvo:** atividade 4 do documento único.

1. Identificar componentes e fronteiras de confiança, incluindo clientes mobile/IoT, API, dados, ML e CI/CD.
2. Revisar Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service e Elevation of Privilege nas fronteiras aplicáveis.
3. Para cada risco, registrar ativo/fluxo, ameaça, impacto/probabilidade com escala explicada, controle, evidência, risco residual e pendência.
4. Incluir riscos DevSecOps de dependências, secrets, código e imagem/deploy e relacionar aos scans.
5. Não declarar assinatura de imagens, SBOM, WAF, APIM, criptografia de coluna ou ownership checks sem evidência. Ausência de controle é lacuna, não autorização para ampliar roadmap.

**Aceite:** revisão substantiva dos riscos e controles atuais, não apenas tarefa anual no calendário; todas as afirmações de implementação têm referência verificável.

## OWASP-1 — Mapeamentos ASVS, Mobile Top 10 e API Top 10 [High]

**Arquivo-alvo:** atividade 4 do documento único.

1. Escolher e registrar versão oficial de cada referência. Conferir IDs e títulos na fonte oficial; não reutilizar a tabela antiga de capítulos ASVS, que não correspondia à versão declarada.
2. Para ASVS, mapear requisitos relevantes aos controles demonstrados; para Mobile e API Top 10, listar as dez categorias da edição escolhida e analisar aplicabilidade ao Ford.
3. Usar tabela `referência/versão/ID | risco no Ford | controle | evidência | estado | pendência`.
4. Estados permitidos: `Implementado e verificado`, `Parcial`, `Planejado`, `Bloqueado`, `N/A justificado`. N/A exige justificativa técnica do item, não ausência do repositório mobile/IoT/ML neste checkout.
5. Não alegar certificação nem atendimento integral ASVS L1/L2. BCrypt ou JWT por si só não provam capítulo inteiro de criptografia/gestão de sessão.

**Aceite:** três mapeamentos presentes, incluindo Mobile Top 10; versões/IDs corretos e evidências coerentes com código após remoções da phase-7.

## LGPD-1 — Mapeamento de dados [High]

**Dependência:** inventário LGPD da phase-1 e integração da phase-8.

Consolidar separadamente dados pessoais, telemetria e localização: origem, finalidade, necessidade, base legal a validar com responsável, acesso, armazenamento/transporte, compartilhamento, retenção/descarte e proteção implementada.

- Mascaramento não é criptografia nem prova automática de anonimização irreversível.
- Não inventar prazo legal de 30 dias ou 5 anos, consentimento universal ou conclusão jurídica de conformidade.
- Direitos do titular e processo de atendimento podem ser documentados; não criar novo produto de autoatendimento/retention engine sem requisito.
- Se houver obrigação legal aplicável que exija mudança adicional, registrar lacuna e encaminhar decisão; não eliminar proteção existente sem análise.

**Aceite:** três classes de dados mapeadas e limitações explícitas. Mencionar apenas PII genérica não atende ao item de telemetria/localização.

## CONT-1 — Quatro rotinas de segurança contínua [Low]

**Arquivo-alvo:** atividade 4 do documento único.

| Rotina exigida | Frequência inicial proposta | Procedimento mínimo | Evidência |
|---|---|---|---|
| Dependências | Semanal | Revisar alertas/PRs, avaliar impacto, testar atualização e registrar decisão | Relatório/PR e resultado dos testes |
| Testes de segurança | A cada PR e revisão mensal | Executar scans e regressões de auth/RBAC/validação; revisar achados | Resultado dos checks e tratamento |
| Permissões | Trimestral e após mudança de perfil | Comparar usuários/perfis e acessos técnicos com matriz aprovada; tratar desvios autorizados | Checklist sem PII e decisões |
| Backup e recuperação | Conferir política existente; testar restauração trimestralmente | Verificar backup configurado, restaurar em ambiente isolado, validar integridade e registrar recuperação | Configuração, log de restore e validação |

Frequências são propostas operacionais para atender rotina, não exigências literais nem justificativa para criar serviços. Identificar responsável real; se indefinido, registrar pendência.

Para backup: reutilizar trabalho já iniciado referido no enunciado; verificar armazenamento, proteção, retenção, acesso e procedimento de recuperação existentes. Não assumir PostgreSQL Flexible Server/PITR sem confirmar ambiente. Restauração nunca deve sobrescrever produção. Documentar limitações/RPO/RTO observados ou aprovados, sem inventar SLA.

**Aceite:** quatro rotinas com frequência, responsável, passos e evidência esperada. Não tornar contratação de pentest, SSL Labs A+, certificação ou programa de drills requisito adicional.

## FINAL-2 — Checklist de conformidade [Low]

**Dependência:** tarefas acima e evidências das outras fases.

Criar tabela no documento com uma linha por requisito do índice `.specs/README.md`, contendo `requisito/linha SEC | estado | evidência | lacuna/responsável`.

1. Conferir todos os itens SEC: pipeline, código/infra, observabilidade e compliance.
2. Distinguir implementação/teste local, validação remota, plano e bloqueio externo.
3. Conferir que não há fluxos removidos anunciados como entregáveis, nem captura fictícia declarada real.
4. Registrar prontidão apenas quando evidências pedidas estiverem disponíveis. `Planejado` atende estado de preparação da spec, mas não equivale à entrega executada da sprint.

**Aceite:** documento único + checklist completo, rastreável e sem declaração de conformidade não demonstrada.

## Checklist

- [ ] FINAL-1: quatro atividades consolidadas.
- [ ] RISK-1: STRIDE + DevSecOps revisados.
- [ ] OWASP-1: ASVS, Mobile Top 10 e API Top 10 mapeados.
- [ ] LGPD-1: dados pessoais, telemetria e localização.
- [ ] CONT-1: quatro rotinas, incluindo backup/recuperação.
- [ ] FINAL-2: checklist com evidências e pendências reais.
