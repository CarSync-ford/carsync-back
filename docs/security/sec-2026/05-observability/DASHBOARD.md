# Evidência de dashboard de segurança

## Resultado da verificação

Em 2026-09-25, consultas somente leitura ao resource group `carsync-dev` retornaram:

- recursos `Microsoft.Portal/dashboards`: **0**;
- recursos `microsoft.insights/workbooks`: **0**;
- Application Insights `appi-carsync-dev`: existente e ligado a `law-carsync-dev`;
- fonte `AppTraces`: 24 `SECURITY_VIOLATION` em 30 dias;
- fonte `AppRequests`: 25 logins HTTP 200 e falhas 400/401/403/500 no mesmo período.

Não foi localizada configuração versionada ou recurso Azure que represente um dashboard de segurança implantado. A interface padrão do portal não foi aberta/capturada por esta execução de terminal. Consequentemente, **não há print real a anexar** e T3.C1 permanece `BLOQUEADO`. Uma imagem reconstruída localmente a partir dos números seria um mockup, não evidência do painel, e não foi criada.

## Painel mínimo proposto — não implantado

Quando houver autorização, criar um único workbook/dashboard equivalente no stack Azure existente; a rubrica não exige quantidade arbitrária de painéis. O painel deve informar ambiente, timezone UTC, última atualização, intervalo e fonte em cada visualização.

| Visualização | Fonte/intervalo | Conteúdo e consulta | Estado atual |
|---|---|---|---|
| Linha do tempo de segurança | `AppTraces`, 24h/7d selecionável | `SECURITY_VIOLATION` por minuto e subtipo derivado; reutilizar classificação de `queries/event-inventory.kql` | dados existem; visualização não salva |
| Resultado de login | `AppRequests`, 24h/7d | `/api/v1/auth` por `ResultCode`/`Success`; não exibir URL completa, usuário ou IP | dados existem; visualização não salva |
| Acesso analítico/retenção | `AppTraces` e `AppMetrics`, 30d | contagem de `ANALYTICS_ACCESS`, `DATA_RETENTION` e `data_retention.removed`; zeros devem permanecer visíveis | emissores existem; ingestão sem ocorrência comprovada |
| Estado do alerta | Azure Monitor | regra, enabled, última avaliação, fired/resolved e action group; separar configuração de instância | regra existe com condição inconsistente; nenhuma instância/30d |
| Lacunas externas | handoffs 03/04 e owner ML | mobile/IoT/ML: fonte, última amostra, atraso e owner | fontes operacionais ausentes |

## Critério da captura obrigatória

O responsável Azure nomeado pelo mantenedor deve:

1. obter autorização para criar/salvar o painel na ferramenta existente;
2. validar KQL após integração do `EVENTS.md` da frente 02;
3. selecionar um intervalo com dados reais e UTC visível;
4. garantir que usuário, IP, e-mail, token, subscription ID e endereço de receiver não apareçam;
5. capturar a tela real em PNG, sem edição de conteúdo além de redaction, e salvar em `docs/security/sec-2026/05-observability/captures/security-dashboard-<data>.png`;
6. registrar recurso, intervalo, fontes, hash SHA-256 da imagem, executor e data em `evidence/T3-C1-dashboard-inventory.json`;
7. repetir a verificação após corrigir/testar a regra. Captura de uma consulta isolada não deve ser descrita como dashboard implantado.

Nenhuma dessas etapas de criação/captura foi fingida nesta entrega.
