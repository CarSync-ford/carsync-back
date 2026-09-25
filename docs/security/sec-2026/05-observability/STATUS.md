# Status — 05-observability

- **Spec / base SHA / responsável / repositório:** `.specs/sec-2026/05-observability.md` / `41d2227eb7f4382a9e58a4696dad76485088bb76` / agente Kiro sob revisão do mantenedor / `git@github.com:CarSync-ford/carsync-back.git`
- **Worktree:** `/home/enzo/College/carsync-sec-2026-05-observability`, branch `sec-2026/05-observability`
- **Data:** 2026-09-25

## T1.C1 — inventário e plano multicomponente

- **Estado:** VERIFICADO (inventário/plano; não equivale a telemetria ausente)
- **Requisito:** R15–R16
- **Arquivos e teste/comando:** `MONITORING.md`; Graphify sobre o grafo de 1.476 nós; inspeção de fontes; consultas Azure somente leitura registradas em `evidence/T1-C1-research.txt`.
- **Resultado observado e data:** em 2026-09-25 foram mapeados API, mobile, IoT e ML com sinal, fonte, regra, destino, responsável e resposta. Valores propostos estão marcados como não vigentes. API possui `AppTraces`; demais componentes têm lacunas explícitas. `EVENTS.md` da frente 02 não existe nesta base.
- **Evidência:** `MONITORING.md` e `evidence/T1-C1-research.txt`; sem PII ou segredos.
- **Dependência externa / responsável / ação para desbloquear:** frente 02 deve publicar `EVENTS.md`; owner mobile/IoT/ML deve ser nomeado e fornecer fontes reais. Mantenedor da entrega integrada coordena a atribuição.

## T2.C1 — consultas sobre eventos efetivamente emitidos

- **Estado:** BLOQUEADO (consultas executadas; contrato `EVENTS.md` ausente)
- **Requisito:** R15–R16
- **Arquivos e teste/comando:** `queries/event-inventory.kql`, `queries/security-events.kql`, `queries/README.md`; execução com `az monitor log-analytics query` contra `AppTraces`; resultado em `evidence/T2-C1-query-results.json`.
- **Resultado observado e data:** em 2026-09-25 as consultas executaram na tabela real e retornaram 24 `SECURITY_VIOLATION` em 30 dias: 5 `Auth Failed`, 16 `JWT Invalid`, 2 `Rate Limit Exceeded` e 1 `Invalid Token`. `ANALYTICS_ACCESS` e `DATA_RETENTION` tiveram zero ocorrências. Rótulos derivados não são apresentados como campos emitidos.
- **Evidência:** JSON parseável e sanitizado; `Message`, IP, usuário, IDs de assinatura e endereço do destinatário não foram retidos.
- **Dependência externa / responsável / ação para desbloquear:** frente 02 deve publicar `docs/security/sec-2026/02-api/EVENTS.md`. Depois da integração, a frente 05 compara nomes/campos/redaction e repete as consultas na versão implantada.

## T2.C2 — regras de alerta e comprovação do estado ativo

- **Estado:** VERIFICADO (configuração documentada; eficácia não verificada)
- **Requisito:** R16
- **Arquivos e teste/comando:** `ALERTS.md`; `az monitor scheduled-query show/list`, `az monitor action-group show/list`, `az monitor metrics alert list`, KQL do limiar e API AlertsManagement; resultado em `evidence/T2-C2-alert-verification.json`.
- **Resultado observado e data:** em 2026-09-25 a regra `alert-sec-violations` e o action group `ag-security-email` estavam habilitados. A condição combina KQL já agregada/filtrada com `Count > 5`, não representando corretamente o objetivo de 5 eventos/minuto. Um bin histórico teve 6 eventos, mas nenhuma instância da regra foi retornada em 30 dias; nenhum disparo/notificação é alegado. Não há regra comprovada para os demais sinais.
- **Evidência:** `ALERTS.md` e JSON sanitizado; regra ativa, regra corrigida proposta e alerta disparado estão explicitamente separados.
- **Dependência externa / responsável / ação para desbloquear:** responsável Azure nomeado pelo mantenedor deve autorizar correção/teste sintético e comprovar instância/entrega. Owners mobile, IoT e ML devem fornecer fontes antes de criar regras.

## T3.C1 — evidência de dashboard de segurança

- **Estado:** BLOQUEADO
- **Requisito:** R17
- **Arquivos e teste/comando:** `DASHBOARD.md`; listagem Azure de `Microsoft.Portal/dashboards` e `microsoft.insights/workbooks`; inventário em `evidence/T3-C1-dashboard-inventory.json`.
- **Resultado observado e data:** em 2026-09-25 foram encontrados zero dashboards e zero workbooks no resource group. Application Insights/workspace e dados reais existem, mas não há painel implantado nem captura real. Nenhum mockup foi usado como substituto.
- **Evidência:** inventário JSON sanitizado; `capture.path` e `sha256` nulos registram explicitamente a ausência exigida.
- **Dependência externa / responsável / ação para desbloquear:** responsável Azure nomeado pelo mantenedor deve obter autorização, salvar o painel equivalente, abrir a ferramenta, capturar tela real sanitizada com intervalo/fonte visíveis e registrar hash/data. Esta execução não recebeu autorização para criar recurso cloud.

## T3.C2 — login, falha e alteração crítica estruturados

- **Estado:** BLOQUEADO (amostras reais parciais; aceite completo ausente)
- **Requisito:** R15
- **Arquivos e teste/comando:** `AUDIT-EVIDENCE.md`, `queries/audit-samples.kql`, `captures/audit/*.json`; consulta a `AppRequests`/`AppTraces` e parse com `python3 -m json.tool`; validação em `evidence/T3-C2-audit-validation.json`.
- **Resultado observado e data:** em 2026-09-25 foram capturadas projeções sanitizadas de um login HTTP 200 real, um `SECURITY_VIOLATION Auth Failed` real e uma tentativa MFA HTTP 500. Login 200 é telemetria automática, não evento de auditoria; a alteração crítica não concluiu; nenhuma ação controlada foi executada nesta sessão.
- **Evidência:** três JSON parseáveis sem mensagem bruta, IP, usuário, token, senha, URL completa ou operation ID.
- **Dependência externa / responsável / ação para desbloquear:** frente 02 deve publicar `EVENTS.md` e instrumentar/verificar sucesso de login e alteração crítica concluída. Mantenedor deve autorizar ambiente/contas sintéticas; então repetir três ações controladas e capturar eventos explícitos.

## T4.C1 — fluxo de resposta em cinco etapas

- **Estado:** VERIFICADO (procedimento documental)
- **Requisito:** R18
- **Arquivos e teste/comando:** `INCIDENT-RESPONSE.md`; verificação das cinco etapas e dos recursos/canais em `evidence/T4-C1-procedure-check.txt`.
- **Resultado observado e data:** em 2026-09-25 o procedimento cobre detecção, análise, contenção, erradicação e recuperação, cada uma com dono/canal, ação, limites e critério de saída. Usa `ag-security-email` e recursos Azure comprovados; não inventa contato nominal. Ações mutáveis exigem aprovação e não foram executadas.
- **Evidência:** procedimento e checklist textual sem PII/segredos.
- **Dependência externa / responsável / ação para desbloquear:** mantenedor deve nomear IC/owners e canal de coordenação antes de uso operacional; owners mobile/IoT/ML permanecem ausentes. Isso limita prontidão, mas não a redação do fluxo.

## T4.C2 — revisão de mesa e consolidação

- **Estado:** VERIFICADO (revisão documental; não incidente/teste produtivo)
- **Requisito:** R18; consolidação R15–R17
- **Arquivos e teste/comando:** `TABLETOP.md`, `REPORT.md`, `evidence/T4-C2-tabletop.json`; KQL somente leitura do bin histórico sanitizado; validação de links/JSON/histórico no fechamento.
- **Resultado observado e data:** em 2026-09-25 o cenário de mesa percorreu as cinco etapas sem gerar tráfego ou alterar produção. A detecção automática falharia pela condição inconsistente; análise manual é possível; contenção/erradicação/recuperação são coerentes, mas não executadas. O exercício não é incidente e não teve participantes humanos operacionais.
- **Evidência:** `TABLETOP.md` e JSON parseável; consolidação/SHAs anteriores em `REPORT.md`.
- **Dependência externa / responsável / ação para desbloquear:** R15–R17 permanecem bloqueados conforme relatório. Mantenedor/owners devem publicar contrato, corrigir/testar alerta, produzir painel/captura e integrar sinais externos. T4.C2 não converte essas lacunas em verificadas.

## Resumo de encerramento desta execução

| Checkpoint | Estado |
|---|---|
| T1.C1 | VERIFICADO — inventário/plano |
| T2.C1 | BLOQUEADO — `EVENTS.md` ausente, apesar de KQL executada |
| T2.C2 | VERIFICADO — configuração auditada; eficácia não verificada |
| T3.C1 | BLOQUEADO — sem dashboard/print real |
| T3.C2 | BLOQUEADO — amostras parciais, sem alteração concluída/contrato |
| T4.C1 | VERIFICADO — procedimento documental |
| T4.C2 | VERIFICADO — revisão de mesa documental |
