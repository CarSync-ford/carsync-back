# Security Checklist & Implementation Plan

**Projeto:** Arquitetura Orientada a Serviços - Sprint 1 (carsync-api-dev)  
**Data:** 2026-09-09  
**Base:** SECURITY_REVIEW_REPORT.md, AZURE_SEC_CHANGES.md, SEC-REQUIREMENTS.md

---

## 1. Pipeline DevSecOps Integrado (Peso 3,0)

### Status Atual
- Pipeline CI/CD existe (`.github/workflows/deploy.yml`): Maven test → Docker build → ACR push → ACA deploy
- **Ausentes:** SAST, SCA, Secret Scanning, Container Security, Documentação do pipeline

### Gaps
- Nenhuma ferramenta SAST (SonarQube, Semgrep)
- Nenhuma análise SCA (Dependabot, Snyk, OWASP Dependency Check)
- Nenhum secret scanning (Gitleaks, GitGuardian)
- Nenhum container scan (Trivy)
- Sem diagrama/documento explicando redução de risco por etapa

---

## 2. Segurança em Código e Infraestrutura (Peso 2,5)

### Implementado ✅
- **Criptografia local:** BCrypt para senhas (`jbcrypt`), criptografia em repouso no PostgreSQL gerenciado (Azure)
- **Validação de entrada:** Bean Validation + validadores custom (`@StrongPassword`, `@ValidCpf`, `@LowercaseEmail`)
- **JWT seguro:** HS256, segredo ≥256 bits validado no `@PostConstruct`, claims completos, anti-timing no login
- **RBAC:** `@EnableMethodSecurity` + `@PreAuthorize("hasRole('USER')")`, role no JWT claim
- **Dockerfile hardening:** usuário non-root, JVM flags, App Insights agent
- **Secrets management:** 100% via env vars + ACA Secrets (GitHub Actions)
- **TLS/HTTPS:** Cloudflare + ACA Ingress (insecure traffic OFF)
- **CORS:** Restrito no ACA Ingress + validação fail-fast no `SecurityConfig`
- **Payload limit:** 1MB multipart
- **Error handling:** Global handler sem stack traces
- **Logs estruturados:** JSON + regex masking (password, cpf, token, secret)
- **Audit trail:** Hibernate Envers em User, UserType, Customer, Lead
- **SQL Injection prevention:** JPA parametrizado apenas
- **HMAC Payload Signing:** Implementado (`HmacSignatureFilter`)
- **Anomaly monitoring:** Logs `SECURITY_VIOLATION` estruturados
- **Role ANALYST & Anonimização:** Migração V6 + endpoints `/api/v1/analytics/**` protegidos com `@PreAuthorize("hasRole('ANALYST')")`, mascaramento PII (`DataMasker`) e log estruturado de auditoria (`ANALYTICS_ACCESS`)
- **Data Retention / Secure Disposal:** Coluna `deleted_at`, soft delete, `DataRetentionService` com expurgo diário (>30 dias) e anonimização semanal (>5 anos sem login), instrumentado com contadores Micrometer `data_retention.removed`

### Parcial ⚠️
- **Rate Limiting:** Bucket4j in-memory (10 req/s/IP) — **não escala em cluster ACA** (cada réplica tem bucket próprio)
- **CORS default:** Fail-fast implementado, mas precisa espelhar config do ACA Ingress

### Ausente ❌
- **MQTT/TLS IoT:** Não aplicável (projeto não tem componente IoT)
- **IaC Security:** Sem Terraform/K8s manifests, sem hadolint/Trivy no CI

---

## 3. Observabilidade, Monitoramento e Resposta (Peso 2,0)

### Implementado ✅
- **Logs estruturados:** JSON no stdout → Log Analytics (`ContainerAppConsoleLogs_CL`)
- **App Insights Agent:** Auto-instrumentação (traces, dependências, métricas runtime)
- **Actuator Probes:** `/actuator/health/liveness` e `/readiness` públicos, demais privados
- **SECURITY_VIOLATION logs:** Rate limit, JWT invalid, auth failed

### Parcial ⚠️
- **KQL Alerts:** Planejados (AZURE_SEC_CHANGES.md §25), não implementados

### Ausente ❌
- **Dashboards** (Azure Monitor/Grafana/Kibana)
- **Plano de Resposta a Incidentes** (Detecção → Análise → Contenção → Erradicação → Recuperação)

---

## 4. Compliance, Riscos e Segurança Contínua (Peso 2,5)

### Implementado ✅
- **LGPD Básico:** PII masking logs, audit trail, RBAC, JWT, secrets mgmt
- **Anonymization Pipeline (SEC-001):** Role `ANALYST`, utilitário `DataMasker`, DTOs analíticos mascarados (`CustomerAnalyticsView`, `LeadAnalyticsView`, `VehicleAnalyticsView`), endpoints `/api/v1/analytics/**` com `@PreAuthorize("hasRole('ANALYST')")`, auditoria estruturada
- **Data Retention / Secure Disposal (SEC-002):** Soft delete com `deleted_at`, jobs `@Scheduled` diário (expurgo >30 dias) e semanal (anonimização >5 anos sem login), métricas Micrometer `data_retention.removed`

### Ausente ❌
- **STRIDE + DevSecOps Risk Review**
- **OWASP ASVS Mapping**
- **OWASP API Top 10 Checklist formal**
- **Rotina revisão dependências** (Dependabot/OWASP Dependency Check)
- **Rotina testes segurança** (SAST/DAST/pentest agendados)
- **Rotina auditoria permissões**
- **Backup/Recovery testado/documentado** (apenas backup automático Azure)

---

## Ordem Sugerida de Implementação

Baseada na arquitetura atual (Spring Boot 3.3, Java 21, ACA, PostgreSQL, GitHub Actions, ACA Secrets, App Insights, Log Analytics). **Front/Mobile testados por último.**

---

### FASE 0 - Quick Wins (1-2 dias) — Já no código, só ativar/ajustar

1. **HMAC enabled por default** — `application.yml:47` já está `true` ✅ (confirmar em prod)
2. **Security Headers** — `SecurityConfig:71-76` já implementado ✅
3. **JWT Secret Validation** — `JwtServiceImpl:27-34` já implementado ✅
4. **CORS Fail-fast** — `SecurityConfig:45-51` já implementado ✅
5. **Validar se HMAC_SECRET está no Container App (ACA)** e injetado no deploy ✅

---

### FASE 1 - LGPD Crítico (Semanas 1-2) — Bloqueia compliance

#### 1.1 Anonymization Pipeline (SEC-001)
- **Migração V6:** `V6__add_analyst_user_type.sql` — insert role `ANALYST` em `user_type`
- **Utilitário:** `br.com.sprint1.challenge.util.DataMasker` (CPF, email, phone, name)
- **DTOs Analytics:** `CustomerAnalyticsView`, `LeadAnalyticsView`, `VehicleAnalyticsView` com campos mascarados
- **Endpoints:** `/api/v1/analytics/**` com `@PreAuthorize("hasRole('ANALYST')")`
- **Testes:** Integração 403 (USER) / 200 (ANALYST) + validação masking
- **Logs:** `INFO ANALYTICS_ACCESS user:{} resource:{}`

#### 1.2 Data Retention / Secure Disposal (SEC-002)
- **Migração V7:** `deleted_at` em `users`, `customers`, `leads`
- **Entidades:** Add `@Column(name = "deleted_at") LocalDateTime deletedAt`
- **Application:** `@EnableScheduling`
- **Config:** `DataRetentionProperties` (`application.yml` bloco `data-retention`)
- **Service:** `DataRetentionService` com 2 jobs:
  - **Job 1 (diário 02:00):** Hard delete `deleted_at > 30 dias`
  - **Job 2 (semanal domingo 03:00):** Anonimização PII usuários inativos `last_login > 5 anos`
- **Métricas:** Micrometer `Counter data_retention.removed`
- **Testes:** Com `Clock` injetado para avançar tempo

---

### FASE 2 - Auth Hardening (Semanas 2-3)

2. **Account Lockout / Brute-force**
   - Migração V8: `failed_login_attempts` (int), `locked_until` (timestamp) em `users`
   - `AuthServiceImpl`: Incrementar falhas, lock após 5 tentativas por 15 min
   - `UserRepository`: Query para unlock automático

3. **Refresh Token Rotation + Expiry**
   - Migração V9: `refresh_token_expires_at` em `users`
   - `AuthServiceImpl`: Rotacionar no uso, revogar na troca de senha
   - TTL configurável (ex: 30 dias)

4. **Password Reset Flow**
   - Endpoint `POST /api/v1/auth/forgot-password` (token JWT curto, 15 min, single-use)
   - Endpoint `POST /api/v1/auth/reset-password` (valida token, atualiza senha)
   - Email delivery (mock ou integração real)

5. **MFA Decision**
   - **Opção A:** Implementar TOTP (RFC 6238) — `google-authenticator` lib
   - **Opção B:** Remover colunas `mfa_secret`, `mfa_enabled` se não usado
   - Decidir baseado em requisito de negócio

---

### FASE 3 - Pipeline DevSecOps (Semanas 3-5)

6. **SAST:** Adicionar **Semgrep** no workflow (gratuito, rápido, OSS)
   ```yaml
   - name: SAST (Semgrep)
     uses: returntocorp/semgrep-action@v1
   ```

7. **SCA:** **OWASP Dependency Check** Maven plugin + **Dependabot** alerts
   ```xml
   <plugin>
     <groupId>org.owasp</groupId>
     <artifactId>dependency-check-maven</artifactId>
     <version>9.0.0</version>
   </plugin>
   ```

8. **Secret Scanning:** **Gitleaks** no CI
   ```yaml
   - name: Secret Scan (Gitleaks)
     uses: gitleaks/gitleaks-action@v2
   ```

9. **Container Security:** **Trivy** scan da imagem Docker
   ```yaml
   - name: Container Scan (Trivy)
     uses: aquasecurity/trivy-action@master
     with:
       image-ref: ${{ env.ACR_LOGIN_SERVER }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
       severity: HIGH,CRITICAL
   ```

10. **Documentação Pipeline:** Criar `docs/security/pipeline-devsecops.md` com diagrama Mermaid + explicação risco/etapa

---

### FASE 4 - Observabilidade & Resposta (Semana 5-6)

11. **KQL Alerts no Azure Monitor** (baseado em AZURE_SEC_CHANGES.md §25)
    - Alert: `SECURITY_VIOLATION` count > threshold em 5 min
    - Alert: Failed login spike
    - Alert: Rate limit excedido repetidamente mesmo IP

12. **Dashboards Azure Monitor / App Insights**
    - Overview: Request rate, error rate, latency (p50/p95/p99)
    - Security: `SECURITY_VIOLATION` timeline, top IPs, auth failures
    - Business: Active users, leads conversion, churn risk distribution

13. **Plano Resposta a Incidentes** — Documento `docs/security/incident-response.md`
    - Fases: Detecção → Análise → Contenção → Erradicação → Recuperação → Lições
    - Runbooks por cenário: Credential stuffing, Token leak, Data breach, DDoS
    - Contatos, escalação, comunicação

---

### FASE 5 - Compliance Formal (Semana 6-7)

14. **STRIDE Threat Model** — Documento `docs/security/threat-model.md`
    - Por componente: API, Auth, Database, Ingress, CI/CD
    - Mitigações mapeadas aos controles implementados

15. **OWASP ASVS Level 1/2 Checklist** — `docs/security/asvs-checklist.md`
    - Marcar cada requisito: Implementado / Parcial / N/A / Planejado

16. **OWASP API Top 10 Mapping** — `docs/security/api-top10.md`
    - API1: Broken Object Level Authorization → RBAC + `@PreAuthorize`
    - API2: Broken Authentication → JWT + BCrypt + Anti-timing
    - API3: Broken Object Property Level Authorization → DTOs separados por role
    - ...etc

17. **Rotinas Contínuas** — Documentar em `docs/security/continuous-security.md`
    - **Semanal:** Dependabot PRs review + merge
    - **Mensal:** OWASP Dependency Check report review
    - **Trimestral:** Permission audit (roles, ACA Secrets/access, ACA RBAC)
    - **Semestral:** Penetration test (interno ou terceirizado)
    - **Anual:** STRIDE review, ASVS re-validação, Incident response drill

18. **Backup/Recovery Test** — Documentar procedimento + teste trimestral
    - Point-in-time restore PostgreSQL
    - Validação integridade dados + auditoria Envers

---

### FASE 6 - Escala & Hardening Contínuo (Contínuo)

19. **Distributed Rate Limiting** — Quando APIM entrar
    - Migrar `RateLimitFilter` para Redis-backed Bucket4j ou remover (APIM assume)
    - ADR documentando decisão temporária in-memory

20. **APIM Integration** — Rate limiting, throttling, quota por subscription
    - Configurar policies no Azure API Management
    - Remover/desabilitar filter interno quando APIM ativo

21. **Cloudflare Full (Strict)** — Validar cert ACA no edge
    - Sem mudança backend, só toggle no painel Cloudflare

---

## Dependências Entre Fases

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

## Estimativa de Esforço Total

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

---

## Critérios de Pronto (Definition of Done) por Fase

### Fase 1 - LGPD
- [x] Migração V6 aplicada (ANALYST role)
- [x] `DataMasker` com 100% cobertura unitária
- [x] Analytics DTOs mascarados funcionando
- [x] Endpoints `/api/v1/analytics/**` retornam 403 para USER, 200 para ANALYST
- [x] Migração V7 aplicada (soft delete)
- [x] `DataRetentionService` com 2 jobs executando (testados com Clock)
- [x] Métricas `data_retention.removed` visíveis no App Insights

### Fase 2 - Auth
- [ ] Lockout após 5 falhas, unlock automático 15 min
- [ ] Refresh token rotaciona a cada uso, expira em 30 dias
- [ ] Password reset flow end-to-end (token 15 min, single-use)
- [ ] MFA decidido e implementado/removido

### Fase 3 - Pipeline
- [ ] SAST roda em todo PR (Semgrep)
- [ ] SCA roda em todo PR (Dependency Check)
- [ ] Secret scan roda em todo PR (Gitleaks)
- [ ] Container scan roda no deploy (Trivy)
- [ ] Documento pipeline com diagrama publicado

### Fase 4 - Observabilidade
- [ ] 3+ KQL alerts ativos no Azure Monitor
- [ ] 2+ dashboards operacionais
- [ ] Plano resposta incidentes aprovado e testado (tabletop)

### Fase 5 - Compliance
- [ ] STRIDE documentado
- [ ] ASVS checklist preenchido
- [ ] API Top 10 mapeado
- [ ] Rotinas contínuas calendarizadas

---

## Notas de Arquitetura para Implementação

- **Multi-tenancy:** Não existe atualmente. Se adicionar, revisar `DataMasker` e `DataRetentionService` para `dealership_id`
- **Event-driven:** Não há message broker. Jobs de retention usam `@Scheduled` (simples, funciona no ACA single instance). Para multi-replica, considerar **ShedLock** ou mover para **Azure Functions Timer Trigger**
- **ACA Secrets:** Já integrado no deploy. Novos secrets (HMAC, JWT rotation) seguem mesmo padrão
- **App Insights:** Já instrumentado. Métricas custom (`Counter`, `Timer`) usam `Micrometer` + `ApplicationInsightsMeterRegistry` (já no classpath via agent)
- **Testes:** Perfil `test` usa H2. Migrações V6-V9 precisam versões H2 em `src/test/resources/db/migration/h2/`
- **Front/Mobile:** Testar integração **após** Fase 1 (anonimização afeta DTOs de resposta) e Fase 2 (auth flow muda)

---

## Próximos Passos Imediatos

1. **Hoje:** Confirmar `HMAC_ENABLED=true` em prod + `HMAC_SECRET` no Container App (ACA)
2. **Esta semana:** Criar migração V6 (ANALYST) + V7 (soft delete) + iniciar `DataMasker`
3. **Paralelo:** Adicionar Semgrep + Gitleaks no workflow (baixo esforço, alto valor)
4. **Próxima sprint:** Focar 100% na Fase 1 (LGPD) — é o bloqueador de compliance