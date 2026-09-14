# Security Review Report

**Project**: Arquitetura Orientada a Serviços - Sprint 1  
**Date**: 2026-09-06  
**Reviewer**: Security Review Skill (OWASP-based)

---

## Executive Summary

| Category | Status | Findings |
|----------|--------|----------|
| **Infrastructure/Crypto** | ✅ Implemented | Handled at Azure/PostgreSQL level |
| **Transport Security (TLS)** | ✅ Implemented | Cloudflare + ACA Ingress, forward-headers |
| **Rate Limiting** | ⚠️ Partial | In-memory only (Bucket4j), no cluster support |
| **CORS** | ⚠️ Partial | Default `*` insecure, needs env config |
| **Monitoring/Observability** | ✅ Implemented | App Insights, Actuator, structured JSON logs |
| **Payload Limits** | ✅ Implemented | 1MB multipart limit |
| **Error Handling** | ✅ Implemented | Global handler, no stack traces |
| **Secrets Management** | ✅ Implemented | Env vars, ACA Secrets, no hardcoded secrets |
| **Authentication (JWT)** | ✅ Implemented | HS256, 256-bit key, anti-timing |
| **Authorization (RBAC)** | ✅ Implemented | Method security, role-based |
| **Audit Trail** | ✅ Implemented | Hibernate Envers on 4 entities |
| **Input Validation** | ✅ Implemented | Bean Validation + custom validators |
| **SQL Injection Prevention** | ✅ Implemented | JPA parametrized queries only |
| **HMAC Payload Signing** | ⚠️ Partial | Implemented but **disabled by default** |
| **Anonymization/PII Masking** | ❌ Missing | Planned only, no DataMasker, no ANALYST role |
| **Secure Disposal/Retention** | ❌ Missing | Planned only, no soft delete, no scheduler |
| **Anomaly Detection** | ✅ Implemented | SECURITY_VIOLATION structured logs |

**Overall Risk Level**: **Medium**  
Critical gaps in anonymization and data retention (LGPD compliance). HMAC disabled by default weakens integrity protection. In-memory rate limiting fails in scaled deployments.

---

## Detailed Findings

### 🔴 CRITICAL - LGPD Compliance Gaps

#### [SEC-001] Anonymization Pipeline Not Implemented
- **Location**: N/A (missing entirely)
- **Requirement**: SECURITY_CHANGES.md § "Anonimização" documents a 6-step plan
- **Current State**: Only log masking (logback-spring.xml) and minimal DTOs exist
- **Missing**:
  - `DataMasker` utility (CPF, email, phone, name masking)
  - `ANALYST` role in `user_type` table (Flyway migration V6)
  - Analytics DTOs (`CustomerAnalyticsView`, etc.) with masked fields
  - `/api/v1/analytics/**` endpoints with `@PreAuthorize("hasRole('ANALYST')")`
  - Integration tests for 403/200 role separation
- **Impact**: Non-compliance with LGPD data minimization; analysts would see full PII
- **Fix**: Implement all 6 steps from SECURITY_CHANGES.md § "Anonimização"

#### [SEC-002] Data Retention / Secure Disposal Not Implemented
- **Location**: N/A (missing entirely)
- **Requirement**: SECURITY_CHANGES.md § "Descarte Seguro" documents 7-step plan
- **Current State**: Only audit tables (Envers) exist; no soft delete, no scheduler
- **Missing**:
  - `deleted_at` column on `users`, `customers`, `leads` (Flyway V7)
  - `@EnableScheduling` on main application
  - `DataRetentionService` with two scheduled jobs:
    - Hard delete of soft-deleted records (>30 days)
    - PII anonymization of inactive users (>5 years)
  - `DataRetentionProperties` configuration block
  - Micrometer metrics for retention operations
- **Impact**: Data kept indefinitely; LGPD Art. 16 retention violation risk
- **Fix**: Implement all 7 steps from SECURITY_CHANGES.md § "Descarte Seguro"

---

### 🟠 HIGH - Security Controls Disabled/Weak by Default

#### [SEC-003] HMAC Payload Signing Disabled by Default
- **Location**: `application.yml:47`, `HmacSignatureFilter.java:35`
- **Current**: `hmac.enabled: ${HMAC_ENABLED:false}` — **disabled unless explicitly enabled**
- **Risk**: Integrity protection inactive in production unless ops team sets `HMAC_ENABLED=true`
- **Evidence**:
  ```yaml
  # application.yml
  hmac:
    secret: ${HMAC_SECRET:}
    enabled: ${HMAC_ENABLED:false}  # <-- DEFAULT FALSE
  ```
- **Fix**: Change default to `true` or document as mandatory production config:
  ```yaml
  hmac:
    enabled: ${HMAC_ENABLED:true}
  ```

#### [SEC-004] In-Memory Rate Limiting (No Cluster Support)
- **Location**: `RateLimitFilter.java:25`
- **Current**: `ConcurrentHashMap<String, Bucket>` — per-instance only
- **Risk**: In ACA with multiple replicas, each replica has independent buckets → effective limit = `replicas × 10 req/s`
- **Fix**: Use distributed Bucket4j (Redis/Hazelcast) or move rate limiting to APIM/Ingress layer as documented

#### [SEC-005] CORS Default Allows All Origins
- **Location**: `SecurityConfig.java:27`, `application.yml:35`
- **Current**: `allowed-origins: ${CORS_ALLOWED_ORIGINS}` with no default in code; but `application.yml` has no fallback
- **Risk**: If env var unset, Spring may default to `*` (check `CorsConfiguration.setAllowedOriginPatterns`)
- **Evidence**: `SecurityConfig.java:73` splits on comma — empty string → `[""]` → effectively `*`
- **Fix**: Require `CORS_ALLOWED_ORIGINS` at startup (fail-fast) or set secure default

---

### 🟡 MEDIUM - Missing Defense-in-Depth

#### [SEC-006] No Account Lockout / Brute-Force Protection
- **Location**: `AuthServiceImpl.java`, `User` entity
- **Missing**: Failed attempt counter, lockout threshold, unlock mechanism
- **Risk**: Credential stuffing / password spray attacks viable
- **Fix**: Add `failed_login_attempts`, `locked_until` columns; increment on failure; lock after 5 attempts for 15 min

#### [SEC-007] Refresh Token No Expiration / Rotation
- **Location**: `User.java:44`, `UserRepository` (no TTL logic visible)
- **Current**: `refresh_token` column stored but no expiry, no rotation on use
- **Risk**: Stolen refresh token = permanent access until password change
- **Fix**: Add `refresh_token_expires_at`; rotate on each use; revoke on password change

#### [SEC-008] MFA Fields Exist But Unused
- **Location**: `User.java:37-41`
- **Current**: `mfa_secret`, `mfa_enabled` columns; no TOTP logic, no enrollment flow
- **Risk**: False sense of security; attack surface if secret leaked
- **Fix**: Either implement TOTP (RFC 6238) or remove unused columns

#### [SEC-009] No Password Reset Flow
- **Location**: N/A (missing)
- **Risk**: Account recovery impossible without admin intervention
- **Fix**: Implement secure reset token (JWT or opaque), email delivery, single-use, short TTL

---

### 🟢 LOW - Hardening Opportunities

#### [SEC-010] JWT Secret Minimum Length Not Enforced at Startup
- **Location**: `JwtServiceImpl.java:27`, `JwtProperties.java`
- **Current**: `Keys.hmacShaKeyFor(secret.getBytes())` — accepts any length
- **Risk**: Weak secret (< 256 bits) reduces HS256 security
- **Fix**: Validate `jwt.secret` length ≥ 32 chars (256 bits) at `@PostConstruct`; fail startup if short

#### [SEC-011] Security Headers Not Explicitly Set
- **Location**: `SecurityConfig.java` (no headers config)
- **Missing**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`
- **Fix**: Add `http.headers(headers -> headers...)` in `SecurityConfig`

#### [SEC-012] Actuator Health Details Hidden But Probes Exposed
- **Location**: `application.yml:58`, `SecurityConfig.java:60`
- **Current**: `show-details: never` (good), but `/actuator/health/liveness|readiness` public
- **Risk**: Low — probes only return UP/DOWN; but consider IP allowlist for probes in production

---

## Requirements Traceability Matrix

| Requirement (from SECURITY_CHANGES.md) | Documented | Implemented | Tested | Gap |
|----------------------------------------|------------|-------------|--------|-----|
| Encryption at Rest | ✅ | ✅ (infra) | N/A | — |
| Force HTTPS/TLS | ✅ | ✅ | — | — |
| Rate Limiting | ✅ | ⚠️ In-memory | ✅ Unit | Cluster support |
| CORS | ✅ | ✅ | — | Default `*` risk |
| Monitoring (App Insights) | ✅ | ✅ | — | — |
| Actuator Probes | ✅ | ✅ | ✅ Unit | — |
| Payload Limit (1MB) | ✅ | ✅ | ✅ Unit | — |
| Error Handling (no stack) | ✅ | ✅ | — | — |
| Structured JSON Logs | ✅ | ✅ | — | — |
| PII Masking in Logs | ✅ | ✅ (regex) | — | — |
| Secrets Management | ✅ | ✅ | — | — |
| JWT Auth (HS256) | ✅ | ✅ | ✅ Unit | — |
| RBAC (@PreAuthorize) | ✅ | ✅ | — | — |
| Audit Trail (Envers) | ✅ | ✅ | — | — |
| Input Validation | ✅ | ✅ | ✅ Unit | — |
| SQL Injection Prevention | ✅ | ✅ (JPA) | — | — |
| HMAC Signing | ✅ | ⚠️ Disabled | ✅ Unit | Default OFF |
| Anonymization | ✅ (plan) | ❌ | ❌ | **Fully missing** |
| Secure Disposal | ✅ (plan) | ❌ | ❌ | **Fully missing** |
| Anomaly Monitoring | ✅ | ✅ (logs) | — | — |

---

## Code Changes Required

### 1. Enable HMAC by Default (SEC-003)
```yaml
# src/main/resources/application.yml
hmac:
  enabled: ${HMAC_ENABLED:true}  # Change false → true
```

### 2. Fail-Fast on Missing CORS Origins (SEC-005)
```java
// SecurityConfig.java - add @PostConstruct validation
@PostConstruct
public void validateCors() {
    if (allowedOrigins == null || allowedOrigins.isBlank() || allowedOrigins.equals("*")) {
        throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set to specific origins");
    }
}
```

### 3. Add JWT Secret Validation (SEC-010)
```java
// JwtServiceImpl.java or JwtProperties.java
@PostConstruct
public void validateSecret() {
    if (jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
        throw new IllegalStateException("JWT secret must be at least 256 bits (32 chars)");
    }
}
```

### 4. Add Security Headers (SEC-011)
```java
// SecurityConfig.java filterChain()
http.headers(headers -> headers
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(FrameOptionsConfig::deny)
    .referrerPolicy(ReferrerPolicyConfig::strictOriginWhenCrossOrigin)
    .permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=()"))
);
```

### 5. Implement Anonymization Pipeline (SEC-001) — **NEW FILES**
```
src/main/java/br/com/sprint1/challenge/util/DataMasker.java
src/main/java/br/com/sprint1/challenge/dto/AnalyticsDtos.java (add masked views)
src/main/java/br/com/sprint1/challenge/controller/AnalyticsController.java (new endpoints)
src/main/resources/db/migration/V6__add_analyst_user_type.sql
src/test/java/br/com/sprint1/challenge/util/DataMaskerTest.java
src/test/java/br/com/sprint1/challenge/controller/AnalyticsControllerTest.java
```

### 6. Implement Data Retention (SEC-002) — **NEW FILES**
```
src/main/java/br/com/sprint1/challenge/config/DataRetentionProperties.java
src/main/java/br/com/sprint1/challenge/service/DataRetentionService.java
src/main/resources/db/migration/V7__add_soft_delete_columns.sql
src/main/java/br/com/sprint1/challenge/ArquiteturaOrientadaaServicosSprint1Application.java (add @EnableScheduling)
src/test/java/br/com/sprint1/challenge/service/DataRetentionServiceTest.java
```

### 7. Add Account Lockout (SEC-006) — **MODIFY EXISTING**
```
src/main/resources/db/migration/V8__add_account_lockout.sql (failed_attempts, locked_until)
src/main/java/br/com/sprint1/challenge/entity/User.java (add fields)
src/main/java/br/com/sprint1/challenge/service/impl/AuthServiceImpl.java (lockout logic)
src/main/java/br/com/sprint1/challenge/repository/UserRepository.java (lockout queries)
```

### 8. Add Refresh Token Rotation (SEC-007) — **MODIFY EXISTING**
```
src/main/resources/db/migration/V9__add_refresh_token_expiry.sql (refresh_token_expires_at)
src/main/java/br/com/sprint1/challenge/entity/User.java (add field)
src/main/java/br/com/sprint1/challenge/service/impl/AuthServiceImpl.java (rotate on use)
src/main/java/br/com/sprint1/challenge/service/impl/UserServiceImpl.java (revoke on password change)
```

---

## Infrastructure Changes Required (per AZURE_SEC_CHANGES.md)

| Item | Status | Action |
|------|--------|--------|
| PostgreSQL Encryption at Rest | ✅ Done | Microsoft-managed keys |
| ACA Ingress Insecure Traffic OFF | ✅ Done | HTTPS only |
| ACA CORS Restriction | ✅ Done | Frontend domain only |
| APIM Rate Limiting | 📋 Planned | Future — replace in-memory filter |
| Log Analytics Workspace | ✅ Done | ContainerAppConsoleLogs_CL |
| App Insights Agent | ✅ Done | Java agent injected |
| Actuator Probes Config | ✅ Done | liveness/readiness paths |
| ACA Secrets Injection | ✅ Done | GitHub Actions deploy |
| PostgreSQL Compute/Storage for Audit | ✅ Done | Sized for _AUD tables |
| Azure Monitor KQL Alerts | 📋 Planned | Alert on SECURITY_VIOLATION |

---

## Test Coverage Gaps

| Area | Current Tests | Missing Tests |
|------|---------------|---------------|
| Rate Limiting | Unit (BucketRateLimitTest) | Integration: cluster scenario, Redis-backed |
| JWT Auth | Unit (JwtServiceTest, AuthServiceTest) | Integration: expired, malformed, wrong key |
| HMAC | Unit (HmacSignatureFilterTest) | Integration: enabled=true, missing header, invalid sig |
| RBAC | None visible | `@PreAuthorize` on all controllers |
| Anonymization | None | **All (new feature)** |
| Data Retention | None | **All (new feature)** |
| Account Lockout | None | **All (new feature)** |
| Refresh Token Rotation | None | **All (new feature)** |

---

## Recommended Priority Order

1. **P0 - LGPD Compliance** (Weeks 1-2)
   - Implement Anonymization pipeline (SEC-001)
   - Implement Data Retention/Disposal (SEC-002)

2. **P1 - Harden Defaults** (Week 1)
   - Enable HMAC by default (SEC-003)
   - Fail-fast CORS config (SEC-005)
   - JWT secret validation (SEC-010)
   - Security headers (SEC-011)

3. **P2 - Auth Hardening** (Weeks 2-3)
   - Account lockout (SEC-006)
   - Refresh token rotation/expiry (SEC-007)
   - Password reset flow (SEC-009)

4. **P3 - MFA Decision** (Week 2)
   - Implement TOTP or remove unused MFA columns (SEC-008)

5. **P4 - Scalability** (Ongoing)
   - Distributed rate limiting (SEC-004) — coordinate with APIM rollout

---

## Documentation Updates Needed

1. **SEC-REQUIREMENTS.md** — Currently **empty**. Populate with formal security requirements matrix mapped to each control above.

2. **SECURITY_CHANGES.md** — Update "Estado atual" sections for Anonimização and Descarte Seguro once implemented.

3. **README.md** — Add Security section with:
   - Required env vars (JWT_SECRET, HMAC_SECRET, CORS_ALLOWED_ORIGINS, etc.)
   - How to run security tests
   - LGPD compliance summary

4. **Architecture Decision Records (ADRs)** — Create for:
   - ADR-001: JWT over opaque tokens
   - ADR-002: HMAC for payload integrity
   - ADR-003: In-memory rate limit (temporary, pre-APIM)
   - ADR-004: Envers for audit trail

---

## Verification Commands

```bash
# Verify HMAC enabled
curl -H "X-HMAC-Signature: invalid" http://localhost:8080/api/v1/user/me
# Should return 401 (not 200/403)

# Verify rate limit (10 req/s)
for i in {1..15}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/health; done
# Should see 429 after 10th request

# Verify CORS rejects unknown origin
curl -H "Origin: https://evil.com" -v http://localhost:8080/api/v1/health 2>&1 | grep "Access-Control-Allow-Origin"
# Should NOT return evil.com

# Verify no stack traces in errors
curl -X POST http://localhost:8080/api/v1/auth -d '{}' -H "Content-Type: application/json"
# Should return clean JSON error, no stack trace

# Verify structured JSON logs
tail -f logs/application.log | jq .
# Should show JSON with masked PII

# Verify Actuator probes public, others private
curl http://localhost:8080/actuator/health/liveness  # 200
curl http://localhost:8080/actuator/env              # 403/401
```

---

## Conclusion

The codebase demonstrates **strong security foundations** with proper JWT auth, RBAC, audit trails, input validation, and infrastructure-level protections (TLS, WAF, ACA Secrets). 

**Two critical LGPD gaps remain unimplemented**: Anonymization pipeline and Data Retention/Disposal. These are fully designed in SECURITY_CHANGES.md but not coded.

**Three high-risk defaults need immediate correction**: HMAC disabled, in-memory rate limiting, permissive CORS default.

Recommend addressing P0/P1 items before production deployment.