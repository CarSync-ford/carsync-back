# Phase 5 - Compliance Formal (Semana 6-7)

**Prioridade:** P2 - Média | **Esforço:** ~1-2 semanas

Consolida tudo anterior. Entregáveis de documentação.

---

## 5.1 STRIDE Threat Model

### Arquivo: `docs/security/threat-model.md`

#### Por Componente

| Componente | Threats (STRIDE) | Mitigações Implementadas |
|------------|------------------|-------------------------|
| **API Layer** | Spoofing: JWT forgery → HS256 256-bit + validation<br>Tampering: Payload modification → HMAC SHA-256<br>Repudiation: No audit → Envers audit trail<br>Info Disclosure: PII in logs → Regex masking<br>DoS: Rate limit bypass → Bucket4j + APIM futuro<br>Elevation: RBAC bypass → `@PreAuthorize` | JWT validation, HMAC, Envers, Log masking, Rate limit, Method security |
| **Auth** | Spoofing: Weak passwords → `@StrongPassword`<br>Tampering: Token replay → Short expiry + rotation<br>Repudiation: No login audit → SECURITY_VIOLATION logs<br>Info Disclosure: User enum → Generic error messages<br>DoS: Brute force → Account lockout (Fase 2)<br>Elevation: Role confusion → Claim-based RBAC | Strong password, Token rotation, Audit logs, Generic errors, Lockout, RBAC |
| **Database** | Spoofing: SQL injection → JPA parametrizado<br>Tampering: Direct DB access → Managed PostgreSQL + SSL<br>Repudiation: No change audit → Envers `_aud` tables<br>Info Disclosure: PII exposure → Column-level encryption (Azure)<br>DoS: Connection exhaustion → Pool config<br>Elevation: Privilege escalation → Least privilege roles | JPA, Azure PostgreSQL, Envers, TDE, Pool, RBAC |
| **Ingress (Cloudflare + ACA)** | Spoofing: TLS termination → Cloudflare Full Strict<br>Tampering: MITM → TLS 1.2+<br>Repudiation: No access logs → Cloudflare logs<br>Info Disclosure: Header leakage → Security headers<br>DoS: Volumetric → Cloudflare WAF + Rate limit<br>Elevation: Path bypass → Path routing rules | TLS, WAF, Headers, Rate limit, Routing |
| **CI/CD** | Spoofing: Compromised runner → GitHub OIDC + least privilege<br>Tampering: Artifact poisoning → Signed images + SBOM<br>Repudiation: No deploy audit → GitHub Actions logs<br>Info Disclosure: Secret leak → Gitleaks + ACA Secrets<br>DoS: Pipeline stall → Timeout + parallel jobs<br>Elevation: Admin access → Branch protection + reviews | OIDC, Signing, Logs, Scanning, Timeouts, Protection |

---

## 5.2 OWASP ASVS Level 1/2 Checklist

### Arquivo: `docs/security/asvs-checklist.md`

Estrutura por capítulo ASVS 4.0:

| Capítulo | Level 1 | Level 2 | Status |
|----------|---------|---------|--------|
| V1: Architecture | ✅ | ✅ | Implementado |
| V2: Authentication | ✅ | ⚠️ Parcial (MFA, lockout Fase 2) | Em progresso |
| V3: Session Management | ✅ | ✅ | JWT stateless |
| V4: Access Control | ✅ | ✅ | RBAC + `@PreAuthorize` |
| V5: Validation | ✅ | ✅ | Bean Validation + custom |
| V6: Stored Crypto | ✅ | ✅ | BCrypt, HS256, ACA Secrets |
| V7: Error Handling | ✅ | ✅ | Global handler, no stack traces |
| V8: Data Protection | ✅ | ⚠️ Parcial (Retention Fase 1) | Em progresso |
| V9: Communications | ✅ | ✅ | TLS, Security headers |
| V10: Malicious Code | ❌ | ❌ | SAST Fase 3 |
| V11: Business Logic | ✅ | ✅ | Domain validation |
| V12: Files/APIs | ✅ | ✅ | Multipart limit, DTOs |
| V13: Config | ✅ | ⚠️ Parcial (IaC Fase 3) | Em progresso |
| V14: Supply Chain | ❌ | ❌ | SCA Fase 3 |

Marcar cada requisito: **Implementado / Parcial / N/A / Planejado**

---

## 5.3 OWASP API Top 10 Mapping

### Arquivo: `docs/security/api-top10.md`

| API Risk | Mitigação no Projeto |
|----------|---------------------|
| **API1: Broken Object Level Authorization** | RBAC + `@PreAuthorize("hasRole('USER')")` + ownership checks |
| **API2: Broken Authentication** | JWT HS256 256-bit, BCrypt, anti-timing, lockout (Fase 2) |
| **API3: Broken Object Property Level Authorization** | DTOs separados por role (Analytics DTOs mascarados Fase 1) |
| **API4: Unrestricted Resource Consumption** | Rate limit 10 req/s/IP, 1MB payload, timeouts |
| **API5: Broken Function Level Authorization** | `@PreAuthorize` por endpoint, ANALYST role (Fase 1) |
| **API6: Unrestricted Access to Sensitive Business Flows** | Churn prediction, lead conversion protegidos por RBAC |
| **API7: Server Side Request Forgery** | Sem outbound HTTP calls user-controlled |
| **API8: Security Misconfiguration** | Security headers, CORS fail-fast, actuator restricted |
| **API9: Improper Inventory Management** | OpenAPI/Swagger versionado, deprecated endpoints removidos |
| **API10: Unsafe Consumption of APIs** | Validação entrada, timeout HTTP clients |

---

## 5.4 Rotinas Contínuas

### Arquivo: `docs/security/continuous-security.md`

| Frequência | Atividade | Responsável |
|------------|-----------|-------------|
| **Semanal** | Dependabot PRs review + merge | Dev Team |
| **Mensal** | OWASP Dependency Check report review | Security |
| **Trimestral** | Permission audit (roles, ACA Secrets, ACA RBAC) | Security + Infra |
| **Semestral** | Penetration test (interno ou terceirizado) | Security |
| **Anual** | STRIDE review, ASVS re-validação, Incident response drill | Security + All |

---

## 5.5 Backup/Recovery Test

### Procedimento documentado
1. **Point-in-time restore** PostgreSQL Flexible Server (Azure Portal / CLI)
2. **Validação integridade:** Contagem registros, constraints FK, auditoria Envers
3. **Teste trimestral** agendado

---

## Critério de Pronto Fase 5

- [ ] STRIDE documentado em `docs/security/threat-model.md`
- [ ] ASVS checklist preenchido em `docs/security/asvs-checklist.md`
- [ ] API Top 10 mapeado em `docs/security/api-top10.md`
- [ ] Rotinas contínuas calendarizadas em `docs/security/continuous-security.md`
- [ ] Backup/Recovery testado e documentado

---

## Notas

- **Evidências:** Commits, PRs, pipeline runs, screenshots dashboards/alerts
- **LGPD:** Fase 1 + 2 cobrem anonimização, retenção, direitos titular
- **Mobile/IoT/ML:** Não aplicável ao backend atual (documentar N/A com justificativa)