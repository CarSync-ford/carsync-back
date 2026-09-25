# Status — 04-iot-infra

- **Spec:** `.specs/sec-2026/04-iot-infra.md`
- **Base SHA:** `41d2227eb7f4382a9e58a4696dad76485088bb76`
- **Responsável pela execução:** agente Kiro, sob revisão do mantenedor
- **Repositório:** `git@github.com:CarSync-ford/carsync-back.git`
- **Worktree:** branch `sec-2026/04-iot-infra`
- **Data da verificação:** 2026-09-25

## Checkpoint T1.C1 — localizar componente IoT, broker, cliente e responsável

- **Estado:** BLOQUEADO
- **Requisito:** R12 — Segurança MQTT/TLS para IoT
- **Arquivos e teste/comando:** `graphify query ... --budget 5000`; busca textual por `mqtt|iot|broker|mosquitto|8883|1883|paho|azure-iot|device|telemetria|tls`; inspeção de remote, branches, histórico, manifests, submódulos e arquivos de ownership. Registro sanitizado em `evidence/T1-C1-research.txt`.
- **Resultado observado e data:** em 2026-09-25, o grafo foi usado como mapa, mas não retornou cliente/broker MQTT efetivo. A inspeção versionada confirmou somente a API Spring e documentação/requisitos sobre IoT. Não há dependência MQTT, configuração de broker, endpoint, porta, CA/truststore, tópico, identidade de dispositivo, submódulo ou repositório IoT referenciado. Assim, protocolo/porta e origem da confiança permanecem **não identificados**, e R12 não está verificado nem é não aplicável.
- **Evidência:** `INVENTORY.md` e `evidence/T1-C1-research.txt`, vinculados à base acima; nenhum valor secreto coletado.
- **Dependência externa / responsável / ação para desbloquear:** o **mantenedor da entrega integrada CarSync/Ford** deve indicar o repositório oficial do firmware/cliente IoT, seu SHA/branch, responsável nominal, broker e ambiente de teste autorizado. O repositório não contém CODEOWNERS nem atribuição nominal do componente IoT; autoria do backend não foi usada para presumir ownership IoT. Após a indicação, criar worktree dedicado no repo IoT antes de qualquer alteração.

## Checkpoint T2.C1 — configurar ou evidenciar TLS no broker/cliente efetivos

- **Estado:** BLOQUEADO (R12 permanece PENDENTE)
- **Requisito:** R12 — Segurança MQTT/TLS para IoT
- **Arquivos e teste/comando:** roteiro em `MQTT-TLS-HANDOFF.md`; pré-validação TLS proposta com `openssl s_client` e teste de conexão pelo runner/biblioteca já existente no repo IoT. Nenhum comando contra cloud, broker compartilhado ou dispositivo foi executado sem autorização.
- **Resultado observado e data:** em 2026-09-25 não foi possível configurar nem demonstrar uma conexão MQTT/TLS positiva, pois T1.C1 não identificou repo/cliente/broker, endpoint, CA, identidade sintética ou ambiente autorizado. Nenhuma configuração deste backend foi alterada e nenhuma infraestrutura fictícia foi criada.
- **Evidência:** `evidence/T2-C1-blocked.txt`; roteiro e critérios em `MQTT-TLS-HANDOFF.md`.
- **Dependência externa / responsável / ação para desbloquear:** mantenedor da entrega integrada deve fornecer os itens de T1.C1 e autorização. O responsável nominal do IoT deverá criar/autorizar o worktree no repo real, confirmar listener TLS e CA de teste e executar o caso positivo com verificação de cadeia e hostname habilitada.
