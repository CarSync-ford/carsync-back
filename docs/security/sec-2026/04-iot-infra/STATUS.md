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

## Checkpoint T2.C2 — rejeição de TLS inválida e ausência de fallback inseguro

- **Estado:** BLOQUEADO (R12 permanece PENDENTE)
- **Requisito:** R12 — Segurança MQTT/TLS para IoT
- **Arquivos e teste/comando:** matriz negativa e comandos sem credenciais em `MQTT-TLS-HANDOFF.md`; registro em `evidence/T2-C2-blocked.txt`.
- **Resultado observado e data:** em 2026-09-25 nenhum teste de CA incorreta, hostname incompatível, listener plaintext ou fallback do cliente foi executado, pois não há componente/ambiente identificado e autorizado. Portanto, não há rejeição TLS real nem ausência de plaintext comprovadas. A não execução evita varredura de endpoint desconhecido e uso indevido de broker/dispositivo.
- **Evidência:** `evidence/T2-C2-blocked.txt`; critérios reproduzíveis no handoff. Nenhuma credencial foi usada ou transmitida.
- **Dependência externa / responsável / ação para desbloquear:** responsável nominal do IoT deve fornecer CA sintética não confiável, hostname/ambiente de teste, identidade sintética e autorização. Executar os casos no worktree do repo IoT, anexar códigos de saída/logs sanitizados e demonstrar que o cliente encerra sem downgrade ou envio plaintext.

## Checkpoint T3.C1 — hardening IaC aplicável no Dockerfile

- **Estado:** REUTILIZADO
- **Requisito:** R13 — demonstração de IaC Security, se aplicável
- **Arquivos e teste/comando:** inspeção de `Dockerfile`; `mvn -B -DskipTests package`; inspeção de nomes/configuração do JAR; consulta ao run 36148042299 e artefato `container-scan-report`; detalhes em `CONTAINER-HARDENING.md` e `evidence/T3-C1-container.txt`.
- **Resultado observado e data:** em 2026-09-25, o pacote local foi gerado com sucesso. Build Docker local ficou bloqueado por permissão no socket. Foi reutilizada a correção da frente 01 no commit `7f735d39fa0eecfa187ddfff1aba931c1b602347`: build remoto e job Trivy concluíram com sucesso; usuário final `spring:spring`, cópia restrita ao JAR, base por digest e checksum do agente foram confirmados. Não há secret em ARG/ENV nem arquivo sensível conhecido no JAR. Trivy não escaneou secrets, e o agente não iniciou telemetria sem connection string; esses limites estão explícitos.
- **Evidência:** `CONTAINER-HARDENING.md`; `evidence/T3-C1-container.txt`; run 36148042299/job 108114377733. Implementação/re-scan pertencem à frente 01.
- **Dependência externa / responsável / ação para desbloquear:** nenhuma para reutilizar o build/re-scan remoto aprovado. A integração deve incluir o Dockerfile da frente 01; qualquer alteração/conflito posterior deve ser notificado à frente 01 e reescaneado. Inspeção local de imagem permanece bloqueada até acesso não privilegiado ao daemon.

## Checkpoint T4.C1 — consolidar evidências e sinais IoT

- **Estado:** VERIFICADO (consolidação documental; R12 continua PENDENTE/BLOQUEADO)
- **Requisito:** R12–R13; insumos para R16 e R21
- **Arquivos e teste/comando:** `REPORT.md`, `HANDOFFS.md`, índice de evidências; validação de links/caminhos, `git diff --check` e Gitleaks.
- **Resultado observado e data:** em 2026-09-25, o relatório consolidou os commits e separou execução local, CI remoto, cloud não executada e IoT/dispositivo não acessados. Falhas/conexões IoT foram encaminhadas à frente 05 como fontes ainda ausentes e schema apenas proposto; telemetria/localização foram encaminhadas à frente 06 como inventário a confirmar, sem afirmar coleta. R12 permanece bloqueado e R13 reutiliza evidência real da frente 01.
- **Evidência:** `REPORT.md`, `HANDOFFS.md`, `evidence/T4-C1-consolidation.txt` e arquivos indexados no relatório.
- **Dependência externa / responsável / ação para desbloquear:** mantenedor da entrega integrada e owner IoT devem fornecer componente/ambiente/autorização para transformar T1/T2 em verificados. Frentes 05/06 devem consumir os handoffs sem tratar propostas como implantação.
