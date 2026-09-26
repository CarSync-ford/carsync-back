# Consultas de eventos reais

As consultas foram executadas em 2026-09-25 contra `AppTraces`, tabela comprovada no workspace `law-carsync-dev` vinculado a `appi-carsync-dev`. O recorte de validação foi de 30 dias. `event-inventory.kql` resume famílias/subtipos e `security-events.kql` retorna detalhe sem a coluna `Message`, que hoje pode carregar IP ou usuário.

## Origem dos nomes

| Literal procurado | Emissor confirmado | Ocorrências em 30 dias |
|---|---|---:|
| `SECURITY_VIOLATION` | `RateLimitFilter`, `JwtAuthenticationFilter`, `GlobalExceptionHandler` | 24 |
| `ANALYTICS_ACCESS` | `AnalyticsController` | 0 |
| `DATA_RETENTION` | `DataRetentionService` | 0 |

Os valores `EventFamily` e `EventSubtype` são rótulos derivados pela consulta a partir desses literais; não são propriedades estruturadas emitidas pela aplicação. Em particular, as consultas **não** pressupõem `AUTH_FAILED`, `RATE_LIMIT_EXCEEDED`, `IP_s` ou qualquer coluna não observada.

Amostras sanitizadas foram comparadas com os literais do código e com a distribuição em `evidence/T2-C1-query-results.json`. O arquivo exigido `docs/security/sec-2026/02-api/EVENTS.md` não existe no `BASE_SHA`; por isso a comparação contratual com a frente 02 permanece bloqueada. Quando ele for integrado, o owner da 05 deve confrontar nomes, campos, redaction e versão antes de promover estas consultas.

## Execução reproduzível

```bash
workspace_id="$(az monitor log-analytics workspace show \
  --resource-group carsync-dev --workspace-name law-carsync-dev \
  --query customerId -o tsv)"
az monitor log-analytics query \
  --workspace "$workspace_id" \
  --timespan P30D \
  --analytics-query "$(cat docs/security/sec-2026/05-observability/queries/event-inventory.kql)"
```

O comando é somente leitura. Não salvar `workspace_id` na evidência e não exportar `Message` sem sanitização.
