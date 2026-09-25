# Relatório — 04-iot-infra

## Resultado executivo

Execução em 2026-09-25, branch `sec-2026/04-iot-infra`, base `41d2227eb7f4382a9e58a4696dad76485088bb76`.

| Requisito | Estado | Conclusão |
|---|---|---|
| R12 — MQTT/TLS para IoT | **PENDENTE / BLOQUEADO** | O componente IoT, cliente, broker, repo/SHA, porta, origem da confiança e responsável nominal não foram identificados. Não há conexão positiva nem rejeição TLS real. A ausência no backend não foi tratada como N/A. |
| R13 — IaC Security aplicável | **REUTILIZADO** | Dockerfile não-root inspecionado; pacote local aprovado. Correções e build/Trivy remotos da frente 01 foram reutilizados com referência imutável e limites explícitos. |

Não foi criado broker, firmware, ponte MQTT, Terraform, Kubernetes, APIM, CDN ou serviço novo. `Dockerfile` e `docker-compose.dev.yml` não foram modificados nesta frente.

## Checkpoints e commits

| Checkpoint | Estado | Commit | Evidência principal |
|---|---|---|---|
| T1.C1 | BLOQUEADO | `1f0f62b` | `INVENTORY.md`, `evidence/T1-C1-research.txt` |
| T2.C1 | BLOQUEADO | `0088e07` | `MQTT-TLS-HANDOFF.md`, `evidence/T2-C1-blocked.txt` |
| T2.C2 | BLOQUEADO | `a926bea` | matriz negativa no handoff, `evidence/T2-C2-blocked.txt` |
| T3.C1 | REUTILIZADO | `9147176` | `CONTAINER-HARDENING.md`, `evidence/T3-C1-container.txt` |
| T4.C1 | VERIFICADO | commit que contém este relatório | `HANDOFFS.md`, `STATUS.md`, índice abaixo |

O commit T4.C1 não referencia o próprio SHA para evitar autorreferência impossível; obtê-lo por `git log -1 --format=%H -- docs/security/sec-2026/04-iot-infra/REPORT.md` após a integração.

## O que foi pesquisado

A consulta graphify foi a primeira fonte para perguntas de arquitetura. No worktree, o grafo versionado tinha 1.476 nós e não encontrou um caminho para cliente/broker MQTT; retornou principalmente API, mobile de exemplo, ML e documentação de TLS HTTPS. Como o grafo é um mapa e pode estar defasado, o resultado foi confrontado com:

- busca textual por MQTT/IoT/broker/SDKs/portas/TLS;
- `pom.xml`, manifests, árvore, branches, remotes, submódulos e histórico completo;
- arquivos de ownership/handoff;
- dois repositórios locais candidatos, apenas para identidade, ambos sem vínculo objetivo com a entrega IoT.

Apenas requisitos/specs mencionam MQTT. TLS em Cloudflare/ACA pertence ao tráfego HTTPS da API e não prova R12. A fase 6 revertida não foi usada como implantação.

## Evidência por ambiente

| Ambiente | Testado | Não testado / limite |
|---|---|---|
| Local — worktree 04 | graphify e inspeção Git; buscas; `mvn -B -DskipTests package`; Dockerfile/JAR; Gitleaks em cada commit | daemon Docker inacessível; nenhum broker/cliente MQTT identificado |
| GitHub Actions — frente 01 | metadados e artefato do run 36148042299; build da imagem corrigida; smoke Java/agent; checksum; Trivy | run de PR, sem deploy; UID/GID não impresso; Trivy não escaneou secrets |
| Cloud IoT / broker compartilhado | nada | sem endpoint, CA, credencial sintética, autorização ou responsável; nenhuma sondagem realizada |
| Dispositivo/firmware real | nada | repo e hardware não fornecidos |
| Azure/Cloudflare de produção | nada | não usados como prova de MQTT nem de deploy desta frente |

## MQTT/TLS — lacuna e aceite restante

Não existe trecho de configuração real a apresentar. O handoff exige, no componente oficial:

1. endpoint TLS real e TLS 1.2+;
2. validação obrigatória da cadeia e hostname/SAN;
3. autenticação existente com secrets fora do código/logs;
4. ACL de tópicos necessária;
5. conexão MQTT positiva no fluxo do projeto;
6. rejeição de CA não confiável e hostname incompatível;
7. prova de que falha TLS encerra sem downgrade, listener plaintext ou envio de credenciais em claro.

`MQTT-TLS-HANDOFF.md` contém pré-validação OpenSSL e matriz negativa. Esses comandos são instruções pendentes, não capturas de execução. Fixture local desconectada poderia complementar diagnóstico, mas não substituiria o caminho integrado; por isso não foi inventada.

## Hardening de container

Controles observados no Dockerfile:

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
COPY target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
ENTRYPOINT ["java", "-javaagent:/opt/agent.jar", "-XX:+DisableAttachMechanism", "-jar", "app.jar"]
```

A frente 01, commit `7f735d39fa0eecfa187ddfff1aba931c1b602347`, preservou esses controles e adicionou base por digest, correção de `libexpat` e checksum do agente. O run 36148042299/job 108114377733 concluiu build e Trivy com zero vulnerabilidades HIGH/CRITICAL reportadas para Alpine/JAR na política configurada.

Limites importantes:

- o build Docker local não foi executado por permissão no socket; não houve `sudo`;
- o smoke carregou o agente, mas ele informou ausência de connection string e não iniciou telemetria;
- essa ausência é compatível com secret não embutido, mas não prova configuração de runtime;
- o relatório Trivy marcou secrets como não escaneados;
- a inspeção estática encontrou `USER spring:spring`, nenhum ARG/ENV de secret e nenhum nome de arquivo sensível conhecido no JAR.

Detalhes e hashes estão em `CONTAINER-HARDENING.md`.

## Encaminhamentos

- **Frente 05 — observabilidade:** consumir `HANDOFFS.md#frente-05--observabilidade`. Não há fonte IoT vigente a afirmar; cliente/broker oficiais devem fornecer eventos de handshake, autenticação, ACL, desconexão e retries com redaction.
- **Frente 06 — compliance:** consumir `HANDOFFS.md#frente-06--compliance-lgpd-e-riscos`. Inventariar telemetria/localização somente após conhecer payload, finalidade, retenção e responsável reais.
- **Mantenedor da integração:** fornecer repo/SHA/owner/ambiente IoT e garantir merge do Dockerfile validado pela frente 01. Mudança posterior nesse arquivo exige novo re-scan pela 01.

## Índice de evidências sanitizadas

| Arquivo | Conteúdo |
|---|---|
| `STATUS.md` | contrato assíncrono e estado por checkpoint |
| `INVENTORY.md` | inventário e bloqueio de ownership MQTT |
| `MQTT-TLS-HANDOFF.md` | roteiro positivo/negativo pendente |
| `CONTAINER-HARDENING.md` | build, inspeções e riscos residuais |
| `HANDOFFS.md` | entradas para frentes 05 e 06 |
| `evidence/T1-C1-research.txt` | comandos/resultados de pesquisa |
| `evidence/T2-C1-blocked.txt` | pré-condições ausentes do caso positivo |
| `evidence/T2-C2-blocked.txt` | pré-condições ausentes dos casos negativos |
| `evidence/T3-C1-container.txt` | build/inspeção local e CI coordenado |
| `evidence/T4-C1-consolidation.txt` | validação documental final do checkpoint |

Nenhuma evidência contém senha, token, chave privada, connection string, endpoint IoT privado, tópico operacional, payload pessoal ou localização.
