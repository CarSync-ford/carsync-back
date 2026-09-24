# 05-observability — monitoramento e resposta

Modelo sugerido: **OpenAI 5.6 sol**. Estado: PENDENTE. Requisitos: R15–R18. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/05-observability` no início do desenvolvimento.

## Base e limites

Há logs SECURITY_VIOLATION, ANALYTICS_ACCESS, métricas de retenção e agente Application Insights. Não há comprovação de painel/alerta implantado. As consultas antigas procuram eventos/campos não demonstrados. Não copiar KQL supondo `AUTH_FAILED`, `RATE_LIMIT_EXCEEDED` ou `IP_s` existentes.

Escrita exclusiva: `docs/security/sec-2026/05-observability/**`, `infra/monitoring/**` apenas se necessário para exportar configuração real. Não editar `src/**`, Dockerfile, pom, workflows ou configurar cloud sem autorização. 02 é dona dos eventos API e produz `EVENTS.md`; 03 e 04 fornecem sinais cliente/IoT. ML deve ter responsável/fonte identificados, mesmo fora deste repo.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Inventariar fontes e plano para API, mobile, IoT e ML | Tabela componente/sinal/fonte/regra/destino/responsável/resposta; ausências explícitas, valores de limiar propostos não fingidos como vigentes | `docs(sec-observability): map monitoring across project components` |
| T2.C1 | Preparar consultas sobre eventos efetivamente emitidos | Consultas na tabela real do ambiente; nomes comparados a amostras sanitizadas e ao EVENTS da 02 | `feat(sec-observability): add queries for actual security events` |
| T2.C2 | Documentar regras de alerta e comprovar o que está ativo | Uma verificação por sinal pertinente, destino e ação; diferenciar regra planejada de alerta disparado | `docs(sec-observability): document alert rules and verification` |
| T3.C1 | Produzir evidência de dashboard de segurança | Painel equivalente na ferramenta existente, captura real sanitizada e intervalo/fonte documentados; não exigir contagem arbitrária de painéis | `docs(sec-observability): capture security dashboard evidence` |
| T3.C2 | Capturar login, falha e alteração crítica estruturados | Amostras JSON parseáveis geradas por ações reais em ambiente autorizado; sem segredos/PII | `docs(sec-observability): capture structured audit evidence` |
| T4.C1 | Redigir fluxo de resposta em cinco etapas | Detecção, análise, contenção, erradicação, recuperação; dono/canal, ação e critério de saída; sem contatos fictícios | `docs(sec-observability): define incident response procedure` |
| T4.C2 | Revisar procedimento com cenário controlado e consolidar | Revisão de mesa identificada como tal, não incidente real; comandos/ações correspondem aos componentes disponíveis | `docs(sec-observability): validate response procedure and evidence` |

## Aceite e assincronia

Plano pode começar agora com inventário real. Consultas e captura API só encerram após contrato de eventos 02; isso não bloqueia plano, dashboard inicial ou resposta. Ausência de acesso à nuvem não impede redigir, mas bloqueia alegação de dashboard/alerta operacional. Prints são exigidos pelo enunciado e não podem ser substituídos por mockups. Sinais sem implementação externa aparecem como lacuna com responsável; não criar coletor/API só para mascarar ausência.

Saídas: `REPORT.md`, `MONITORING.md`, `INCIDENT-RESPONSE.md`, `STATUS.md`, consultas e capturas na pasta exclusiva. Plano final inclui todos os quatro componentes sem inventar telemetria.
