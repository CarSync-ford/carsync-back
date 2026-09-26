# 04-iot-infra — MQTT/TLS e infraestrutura aplicável

Modelo sugerido: **OpenAI 5.6 terra**. Estado: PENDENTE. Requisitos: R12–R13; insumos R14, R16, R21. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/04-iot-infra` no início do desenvolvimento; alterações em outro repo IoT também exigem worktree dedicado.

## Base e limites

Há Dockerfile não-root e agente Application Insights. Não há prova de MQTT/TLS operante nesta API. Ausência de broker neste repositório não torna o requisito IoT inaplicável ao projeto integrado. APIM/Cloudflare da fase 6 revertida não servem como prova de implantação.

Escrita exclusiva: `Dockerfile`, `docker-compose.dev.yml` somente quando necessário ao controle demonstrado, `docs/security/sec-2026/04-iot-infra/**`. Configuração do broker/dispositivo deve ser alterada no repo real identificado; não inventar infraestrutura no backend. Pipeline/scans pertencem a 01; configuração Spring pertence a 02.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Localizar componente IoT, broker, cliente e responsável | Repo/SHA, protocolo/porta e origem da confiança; valores secretos omitidos; sem acesso, registrar bloqueio | `docs(sec-iot): identify mqtt tls evidence owner` |
| T2.C1 | Configurar ou evidenciar TLS no broker/cliente efetivos | Conexão válida com verificação de cadeia/hostname habilitada; trecho de configuração e resultado | `fix(sec-iot): enforce mqtt tls validation` |
| T2.C2 | Verificar rejeição TLS inválida e ausência de fallback inseguro | Teste autorizado/local com CA ou hostname incorreto falha; credenciais não são enviadas em plaintext; resultado sanitizado | `test(sec-iot): verify mqtt tls rejection` |
| T3.C1 | Evidenciar hardening IaC aplicável no Dockerfile existente | Build e inspeção do usuário/segredos; controles reais explicados; corrigir somente defeito necessário confirmado; **notificar 01 se houver mudança no Dockerfile para re-scan** | `docs(sec-infra): evidence existing container hardening` |
| T4.C1 | Consolidar trechos, capturas, commits e sinais IoT | REPORT distingue testado/local/cloud; encaminha fonte de falhas de conexão a 05 e dados de telemetria/localização a 06 | `docs(sec-iot): record infrastructure security evidence` |

## Aceite e não escopo

R12 exige evidência no caminho MQTT usado pelo projeto, não apenas HTTPS na API. Sem repo/ambiente IoT, fornecer passos e responsável, manter pendente. Não exigir mTLS se TLS autenticado e controles existentes satisfazem a necessidade; nunca desligar validação de certificado para fazer teste passar. Não exigir Terraform, Kubernetes, Key Vault, APIM ou CDN novos. Secrets ACA/env existentes podem ser documentados; não migrar serviço por preferência.

Execuções sobre cloud, certificados, broker compartilhado ou dispositivo real dependem de autorização do responsável. Saídas: `REPORT.md`, `STATUS.md`, capturas sanitizadas e referências versionadas na pasta exclusiva.
