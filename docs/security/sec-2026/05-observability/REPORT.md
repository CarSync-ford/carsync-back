# Relatório — observabilidade, monitoramento e resposta

**Spec:** `05-observability` · **requisitos:** R15–R18 · **base:** `41d2227eb7f4382a9e58a4696dad76485088bb76` · **branch:** `sec-2026/05-observability` · **data:** 2026-09-25.

## Resultado executivo

A API tem telemetria real em Azure: `AppTraces` confirmou 24 `SECURITY_VIOLATION` em 30 dias e `AppRequests` confirmou tráfego de login. Uma regra de alerta e um action group estão habilitados, mas a condição combina duas agregações incompatíveis; houve um bin com seis eventos e nenhuma instância de alerta foi encontrada. Não há dashboard/workbook customizado nem print real. O contrato `EVENTS.md`, eventos explícitos de sucesso/alteração concluída e fontes operacionais mobile/IoT/ML estão ausentes.

Nada foi configurado na nuvem. Todas as consultas foram somente leitura, saídas foram sanitizadas e nenhuma lacuna foi substituída por mockup, evento inventado ou coletor novo.

## Estado por requisito

| Requisito | Estado | Evidência e limite |
|---|---|---|
| R15 — logs estruturados de login, falhas e alterações críticas | **BLOQUEADO** | falha explícita e login HTTP real capturados; sucesso não é evento de aplicação, alteração crítica não concluiu, encoder não garante JSON e `EVENTS.md` está ausente |
| R16 — métricas e alertas API/mobile/IoT/ML | **BLOQUEADO/PARCIAL** | API tem logs e regra ativa com defeito; métrica de retenção não apareceu; demais componentes não têm fonte/owner operacional comprovado |
| R17 — dashboard e print | **BLOQUEADO** | zero dashboards/workbooks; roteiro definido, nenhuma captura/mockup criada |
| R18 — resposta em cinco etapas | **VERIFICADO documentalmente** | procedimento e revisão de mesa completos; detecção automática falhou no cenário e prontidão depende de owners/canal/teste autorizado |

## Entregas

- `MONITORING.md`: inventário fonte/regra/destino/responsável/resposta para API, mobile, IoT e ML.
- `queries/`: KQL validada em `AppTraces`/`AppRequests`, sem presumir campos históricos.
- `ALERTS.md`: regra/destino ativos, sem confundir configuração com disparo.
- `DASHBOARD.md`: inventário real e critério para painel/captura; bloqueio explícito.
- `AUDIT-EVIDENCE.md` e `captures/audit/*.json`: amostras reais sanitizadas e limites.
- `INCIDENT-RESPONSE.md`: detecção → análise → contenção → erradicação → recuperação.
- `TABLETOP.md`: revisão de mesa identificada como exercício, não incidente.
- `STATUS.md`: contrato assíncrono por checkpoint; `evidence/`: resultados parseáveis/sanitizados.

## Evidência operacional resumida

| Verificação somente leitura | Resultado |
|---|---|
| Container App | `carsync-api-dev` Running/Succeeded |
| Application Insights/workspace | `appi-carsync-dev` → `law-carsync-dev`, 30 dias de retenção |
| `AppTraces`/30d | 23.713 traces; 24 `SECURITY_VIOLATION`; 0 `ANALYTICS_ACCESS`; 0 `DATA_RETENTION` |
| Subtipos | 5 `Auth Failed`, 16 `JWT Invalid`, 2 `Rate Limit Exceeded`, 1 `Invalid Token` |
| `AppMetrics` | 0 linhas de `data_retention.removed` |
| Login `AppRequests` | 25 HTTP 200; falhas 400/401/403/500 observadas |
| Regra/destino | `alert-sec-violations` e `ag-security-email` habilitados; condição inconsistente |
| Instâncias da regra | 0 em 30 dias; nenhum disparo/notificação alegado |
| Dashboards/workbooks | 0 / 0 |

IDs de assinatura, endereço do receiver, IP, usuário, mensagem bruta, URL completa, operation ID, tokens e credenciais não foram retidos no relatório.

## Commits atômicos

| Checkpoint | Commit |
|---|---|
| T1.C1 | `f850ab29be20b0d86f17023b8a3aef1701e0bd31` |
| T2.C1 | `ce487124efa014b033b2d9dcbe224435feabd0b6` |
| T2.C2 | `4df21f154bb7eac8d254dade23c95cbad84cda06` |
| T3.C1 | `96bff1a81a32a5a65a275d7cfdd67feee96fd4bd` |
| T3.C2 | `8b7184bbbd454a59008de5ba7bfe062d656a3e01` |
| T4.C1 | `0cf102cfe9ac93f7cb2dfcfdef25711c57679d2b` |
| T4.C2 | commit que adiciona este relatório; consultar `git log -1` para evitar autorreferência impossível |

## Ações para encerramento do aceite

1. Frente 02 publica `EVENTS.md`, corrige/valida JSON e instrumenta sucesso/alteração crítica concluída.
2. Owner Azure corrige e testa a regra com ação sintética autorizada e comprova instância/entrega.
3. Owner Azure cria/salva painel equivalente e anexa captura real sanitizada com hash/intervalo/fonte.
4. Mantenedor nomeia IC, canal e owners mobile/IoT/ML; cada owner fornece fonte, baseline e teste real.
5. Reexecutar KQL e revisão operacional na versão integrada; não promover estados bloqueados apenas porque os documentos existem.
