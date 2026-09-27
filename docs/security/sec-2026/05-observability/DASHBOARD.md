# Evidência de dashboard de segurança

## Recurso implantado no Azure

Em 2026-09-26, foi provisionado e validado o recurso nativo **Azure Monitor Workbook**:

- **ID do recurso:** `/subscriptions/7fd8132e-7c9a-4b4d-a191-04b21ecc968c/resourceGroups/carsync-dev/providers/Microsoft.Insights/workbooks/24de0afb-9527-4da1-805b-3ae29db1eb83`
- **Nome de exibição:** `CarSync — Segurança e Operação`
- **Resource Group:** `carsync-dev` (Location: `eastus`)
- **Fonte de dados vinculada:** `appi-carsync-dev` (Application Insights) / `law-carsync-dev` (Log Analytics Workspace `72dac82d-b8c4-4685-952a-c0d263713c83`)
- **Link direto no Azure Portal:** [Abrir Workbook](https://portal.azure.com/#@11dbbfe2-89b8-4549-be10-cec364e59551/resource/subscriptions/7fd8132e-7c9a-4b4d-a191-04b21ecc968c/resourceGroups/carsync-dev/providers/Microsoft.Insights/workbooks/24de0afb-9527-4da1-805b-3ae29db1eb83)
- **Definição ARM/JSON versionada:** `docs/security/sec-2026/05-observability/workbook.json`

## Painéis e consultas configuradas (12 itens)

O Workbook é composto por cabeçalho explicativo, seletor de período (`TimeRange`: 24h padrão, 7d e 30d opcionais) e 7 visualizações nativas, cada gráfico temporal acompanhado de tabela de dados acessível:

| Painel | Visualização | Consulta KQL / Lógica | Telemetria real observada |
|---|---|---|---|
| **Volume de requisições estimado** | Gráfico temporal + tabela | `AppRequests \| summarize Requisicoes=sum(Weight) by bin(TimeGenerated, 1h)` | 5 bins na janela 24h |
| **Erros HTTP 5xx estimados** | Gráfico temporal + tabela | `AppRequests \| summarize Erros5xx=sumif(Weight, toint(ResultCode) between (500 .. 599)) by bin(TimeGenerated, 1h)` | 5 bins na janela 24h |
| **Latência p95 observada (ms)** | Gráfico temporal + tabela | `AppRequests \| summarize P95_ms=percentile(DurationMs, 95) by bin(TimeGenerated, 1h)` | 5 bins na janela 24h |
| **Respostas 401, 403 e 429** | Tabela ranqueada | `AppRequests \| where ResultCode in ("401", "403", "429") \| summarize Estimativa=sum(Weight), Registros=count() by ResultCode` | 2 códigos ativos (401 e 403) |
| **Rotas com erros (4xx e 5xx)** | Tabela ranqueada | Agrupamento por rota normalizada (`/api/v1/leads/{id}`, etc.) e `ResultCode`, ocultando query strings e IDs | 20 combinações observadas |
| **Login e ações sensíveis** | Tabela | Classificação HTTP de `/auth`, `/change-password`, `/reset-password` e `/mfa/*`. Rótulo explícito: telemetria HTTP operacional, não auditoria de domínio | 7 ações/resultados |
| **Eventos sanitizados** | Tabela | Extração de `Familia` e `Tipo` de `AppTraces` com `SECURITY_VIOLATION`, `ANALYTICS_ACCESS` e `DATA_RETENTION`. Coluna `Message` bruta, IPs e emails **não projetados** | 2 eventos sanitizados |

## Regras de sanitização e proteção de dados aplicadas

1. **Zero vazamento de PII:** As colunas `Message`, `Url` bruta, `UserId`, `ClientIP` e parâmetros de query nunca são projetadas nas visualizações do Workbook.
2. **Normalização de rotas:** Parâmetros de rota (`/api/v1/leads/-1`, `/api/v1/customers/123/360`) são normalizados com regex para `{id}` antes da agregação.
3. **Ponderação por amostragem:** Utiliza `Weight = coalesce(ItemCount, 1)` para projeção volumétrica precisa em caso de sampling do SDK.
4. **Disclaimers operacionais:** O painel informa explicitamente que respostas HTTP não comprovam transação de negócio e que mobile, IoT e ML não possuem SDK conectado nesta fonte.

## Instrução para captura de tela (evidência visual R17)

Para produzir a evidência de print exigida por `SEC-REQUIREMENTS.md`:

1. Acessar o link do Azure Portal autenticado com credenciais de leitor/colaborador do tenant `11dbbfe2-89b8-4549-be10-cec364e59551`.
2. Selecionar o período `Últimas 24 horas` no seletor do topo.
3. Capturar a janela inteira mostrando o título `CarSync — Segurança e Operação`, os gráficos de requisições, erros 5xx e as tabelas de eventos.
4. Salvar a captura sanitizada (ocultando subscription ID e barra de usuário) em `docs/security/sec-2026/05-observability/captures/dashboard/dashboard-carsync-2026-09-26.png`.
