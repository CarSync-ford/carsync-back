# Phase 6 - Escala & Hardening Contínuo (Contínuo)

**Prioridade:** P3 - Baixa | **Esforço:** Contínuo

Executar quando APIM estiver pronto / necessidade de escala real.

---

## 6.1 Distributed Rate Limiting

### Problema Atual
- `RateLimitFilter` usa `ConcurrentHashMap<String, Bucket>` local
- Cada réplica ACA tem bucket próprio → limite efetivo = N réplicas × 10 req/s

### Solução: Migrar para Redis-backed Bucket4j

#### Dependência
```xml
<dependency>
  <groupId>com.giffing.bucket4j.spring</groupId>
  <artifactId>bucket4j-spring-boot-starter</artifactId>
  <version>1.5.0</version>
</dependency>
<dependency>
  <groupId>com.giffing.bucket4j.spring</groupId>
  <artifactId>bucket4j-redis</artifactId>
  <version>1.5.0</version>
</dependency>
```

#### Configuração: `application.yml`
```yaml
bucket4j:
  redis:
    enabled: true
  filters:
    - filter-name: api-rate-limit
      url: "/*"
      rate-limits:
        - bandwidth: 10
          duration: 1s
          key: "ip"
```

#### ADR: Documentar decisão temporária in-memory
Arquivo: `docs/adr/001-in-memory-rate-limiting.md`

---

## 6.2 APIM Integration

### Azure API Management assume:
- Rate limiting, throttling, quota por subscription
- JWT validation na borda
- Transformação request/response
- Analytics centralizado

### Passos
1. Provisionar APIM (Consumption ou Developer tier)
2. Importar OpenAPI spec (`/v3/api-docs`)
3. Configurar policies:
   ```xml
   <policies>
     <inbound>
       <validate-jwt header-name="Authorization" failed-validation-httpcode="401">
         <openid-config url="https://login.microsoftonline.com/{tenant}/v2.0/.well-known/openid-configuration" />
         <audiences>
           <audience>{client-id}</audience>
         </audiences>
       </validate-jwt>
       <rate-limit calls="100" renewal-period="60" counter-key="@(context.Request.IpAddress)" />
     </inbound>
   </policies>
   ```
4. Atualizar ACA Ingress: permitir apenas tráfego do APIM (VNet integration ou IP restriction)
5. Remover/desabilitar `RateLimitFilter` interno quando APIM ativo

---

## 6.3 Cloudflare Full (Strict)

### Validação
- Cloudflare já termina TLS (Flexible/Full)
- **Full (Strict):** Cloudflare valida certificado do origin (ACA)
- Sem mudança no backend, apenas toggle no painel Cloudflare

### Passos
1. Gerar certificado válido para ACA (managed certificate ou custom)
2. No Cloudflare: SSL/TLS → Overview → Full (Strict)
3. Verificar: `curl -v https://app.domain.com` → cert chain válido

---

## 6.4 Hardening Contínuo (Backlog)

### Itens para revisar periodicamente

| Item | Frequência | Ação |
|------|------------|------|
| Dependency updates | Semanal | Dependabot PRs |
| CVE scan | Mensal | Trivy + Dependency Check |
| Permission audit | Trimestral | ACA Secrets, ACA RBAC, APIM, PostgreSQL roles |
| Penetration test | Semestral | Interno/terceirizado |
| TLS config review | Semestral | SSL Labs A+ target |
| Secret rotation | Anual | JWT, HMAC, DB passwords |
| STRIDE review | Anual | Atualizar threat model |
| Incident response drill | Anual | Tabletop exercise |

---

## Critério de Pronto Fase 6

- [ ] Distributed rate limiting operacional (quando multi-replica)
- [ ] APIM integrado e assumindo rate limit/auth na borda
- [ ] Cloudflare Full (Strict) validado
- [ ] Backlog de hardening contínuo calendarizado

---

## Dependências Entre Fases (Recap)

```
FASE 0 (Quick Wins) 
    ↓
FASE 1 (LGPD Crítico) ← Independente, pode rodar em paralelo com FASE 2
    ↓
FASE 2 (Auth Hardening) ← Precisa migrações V8, V9 (pós V7)
    ↓
FASE 3 (Pipeline DevSecOps) ← Independente, pode iniciar cedo
    ↓
FASE 4 (Observabilidade) ← Precisa logs SECURITY_VIOLATION (já existem)
    ↓
FASE 5 (Compliance Formal) ← Consolida tudo anterior
    ↓
FASE 6 (Escala) ← Quando APIM pronto
```

---

## Estimativa Total

| Fase | Semanas | Prioridade |
|------|---------|------------|
| 0 - Quick Wins | 0.5 | Imediato |
| 1 - LGPD Crítico | 2 | **P0 - Bloqueante** |
| 2 - Auth Hardening | 2 | **P1 - Alta** |
| 3 - Pipeline DevSecOps | 2-3 | **P1 - Alta** |
| 4 - Observabilidade | 1-2 | **P2 - Média** |
| 5 - Compliance Formal | 1-2 | **P2 - Média** |
| 6 - Escala | Contínuo | **P3 - Baixa** |

**Total: ~7-10 semanas** para compliance LGPD + DevSecOps maduro