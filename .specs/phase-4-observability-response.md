# Phase 4 - Observabilidade & Resposta (Semana 5-6)

> HISTÓRICO — substituída como backlog desta entrega por [SEC-2026](sec-2026/README.md). Não executar cumulativamente. Checkboxes e alegações abaixo não comprovam o estado atual.

**Prioridade:** P2 - Média | **Esforço:** ~1-2 semanas

Dependência: Logs `SECURITY_VIOLATION` já existem no código.

---

## 4.1 KQL Alerts no Azure Monitor

Base: `AZURE_SEC_CHANGES.md` §25

### Alertas a criar

#### Alert 1: SECURITY_VIOLATION Spike
```kql
ContainerAppConsoleLogs_CL
| where Log_s has "SECURITY_VIOLATION"
| summarize count() by bin(TimeGenerated, 5m)
| where count_ > 10
```
- **Severidade:** High
- **Ação:** Notificar team Security + On-call

#### Alert 2: Failed Login Spike
```kql
ContainerAppConsoleLogs_CL
| where Log_s has "AUTH_FAILED" or Log_s has "LOGIN_FAILED"
| summarize count() by bin(TimeGenerated, 5m), IP_s
| where count_ > 20
```
- **Severidade:** High
- **Ação:** Bloquear IP temporário no Cloudflare/APIM

#### Alert 3: Rate Limit Excedido Repetidamente
```kql
ContainerAppConsoleLogs_CL
| where Log_s has "RATE_LIMIT_EXCEEDED"
| summarize count() by bin(TimeGenerated, 5m), IP_s
| where count_ > 50
```
- **Severidade:** Medium
- **Ação:** Investigar possível ataque DDoS/credential stuffing

#### Alert 4: HMAC Validation Failures
```kql
ContainerAppConsoleLogs_CL
| where Log_s has "HMAC_INVALID" or Log_s has "HMAC_MISSING"
| summarize count() by bin(TimeGenerated, 5m)
| where count_ > 5
```
- **Severidade:** Medium
- **Ação:** Possível tentativa de replay/forgery

---

## 4.2 Dashboards Azure Monitor / App Insights

### Dashboard 1: Overview Operacional
- Request rate (req/s)
- Error rate (%)
- Latency p50 / p95 / p99
- Active instances (ACA replicas)

### Dashboard 2: Security
- `SECURITY_VIOLATION` timeline (últimas 24h)
- Top 10 IPs por violações
- Auth failures por tipo (JWT invalid, HMAC missing, rate limit)
- Geographic map de IPs suspeitos

### Dashboard 3: Business
- Active users (últimos 30 min)
- Leads conversion rate
- Churn risk distribution (LOW/MEDIUM/HIGH)
- Vehicle interactions volume

---

## 4.3 Plano de Resposta a Incidentes

### Arquivo: `docs/security/incident-response.md`

#### Fases (NIST SP 800-61)
1. **Detecção** - Alertas KQL, logs, relatórios usuários
2. **Análise** - Triagem, classificação severidade, escopo
3. **Contenção** - Curto prazo (block IP, revogar token), Longo prazo (patch, config)
4. **Erradicação** - Remover causa raiz, limpar sistemas
5. **Recuperação** - Restore, validação, monitoramento reforçado
6. **Lições Aprendidas** - Post-mortem, atualizar runbooks

#### Runbooks por Cenário
| Cenário | Detecção | Contenção Imediata |
|---------|----------|-------------------|
| Credential Stuffing | Failed login spike | Block IP no Cloudflare, enable MFA forçado |
| Token Leak | Logs HMAC/JWT anômalos | Rotacionar secrets (JWT, HMAC), revogar tokens |
| Data Breach | Alert PII access anômalo | Isolar instância, audit trail Envers, notificar DPO |
| DDoS | Rate limit spike + latency | Cloudflare Under Attack mode, APIM throttle |

#### Contatos & Escalação
- Security Team: security@company.com
- On-call: rotação semanal
- DPO: dpo@company.com (LGPD)
- Azure Support: caso infra

---

## Critério de Pronto Fase 4

- [ ] 3+ KQL alerts ativos no Azure Monitor
- [ ] 2+ dashboards operacionais publicados
- [ ] Plano resposta incidentes aprovado e testado (tabletop exercise)

---

## Notas

- **App Insights:** Já instrumentado (agent auto). Métricas custom usam `Micrometer` + `ApplicationInsightsMeterRegistry`
- **Log Analytics:** Workspace já recebe `ContainerAppConsoleLogs_CL`
- **Regex masking:** `logback-spring.xml` já mascara password, cpf, token, secret