# Implementation Plan: P1 Harden Defaults Bundle

**Priority**: P1 (Week 1)  
**Scope**: 4 independent security hardening changes  
**Testing**: TDD - Unit + Integration tests for each change

---

## Change 1: SEC-003 - Enable HMAC by Default (Exclude Actuator Probes)

### Files to Modify

| File | Change |
|------|--------|
| `src/main/resources/application.yml` | Line 47: `enabled: ${HMAC_ENABLED:true}` (was `false`) |
| `src/main/java/.../config/HmacSignatureFilter.java` | Add `/actuator/health/liveness`, `/actuator/health/readiness` to `PUBLIC_PREFIX_PATHS` |

### Rationale
- Default `true` enforces integrity protection in production
- Actuator probes MUST remain public for ACA liveness/readiness checks
- Current `PUBLIC_PREFIX_PATHS` only has `/actuator/health/` (prefix) — need explicit probe paths

### Tests

**Unit**: `HmacSignatureFilterTest`
- `shouldNotFilter_actuatorLiveness_returnsTrue()`
- `shouldNotFilter_actuatorReadiness_returnsTrue()`
- `shouldNotFilter_actuatorHealthPrefix_returnsTrue()`
- `doFilterInternal_enabledTrue_missingHeader_returns401()`
- `doFilterInternal_enabledTrue_validSignature_passes()`

**Integration**: `HmacSignatureFilterIntegrationTest` (@SpringBootTest + MockMvc)
- `GET /actuator/health/liveness` → 200 (no HMAC required)
- `GET /actuator/health/readiness` → 200 (no HMAC required)
- `POST /api/v1/user` (signup) → 200 (no HMAC required, public POST)
- `GET /api/v1/user/me` → 401 without HMAC header
- `GET /api/v1/user/me` with valid HMAC → 200

---

## Change 2: SEC-005 - Fail-Fast CORS Validation

### Files to Modify

| File | Change |
|------|--------|
| `src/main/java/.../config/SecurityConfig.java` | Add `@PostConstruct validateCors()` method |

### Implementation
```java
@PostConstruct
public void validateCors() {
    if (allowedOrigins == null || allowedOrigins.isBlank() || allowedOrigins.equals("*")) {
        throw new IllegalStateException(
            "CORS_ALLOWED_ORIGINS must be set to specific origins (comma-separated), not '*' or empty"
        );
    }
}
```

### Tests

**Unit**: `SecurityConfigTest`
- `validateCors_blank_throwsIllegalStateException()`
- `validateCors_wildcard_throwsIllegalStateException()`
- `validateCors_validOrigins_doesNotThrow()`

**Integration**: `SecurityConfigIntegrationTest`
- Start context with `CORS_ALLOWED_ORIGINS=https://app.example.com` → context loads
- Start context with `CORS_ALLOWED_ORIGINS=*` → context fails to start
- Start context with unset `CORS_ALLOWED_ORIGINS` → context fails to start

---

## Change 3: SEC-010 - JWT Secret Minimum Length Validation

### Files to Modify

| File | Change |
|------|--------|
| `src/main/java/.../service/impl/JwtServiceImpl.java` | Add `@PostConstruct validateSecret()` |

### Implementation
```java
@PostConstruct
public void validateSecret() {
    byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
        throw new IllegalStateException(
            "JWT secret must be at least 256 bits (32 characters) for HS256 security"
        );
    }
}
```

### Tests

**Unit**: `JwtServiceImplTest`
- `validateSecret_32chars_doesNotThrow()`
- `validateSecret_31chars_throwsIllegalStateException()`
- `validateSecret_empty_throwsIllegalStateException()`
- `generateToken_validSecret_returnsToken()`
- `parse_validToken_returnsClaims()`

**Integration**: `JwtServiceIntegrationTest`
- Start context with `JWT_SECRET=short` → context fails to start
- Start context with `JWT_SECRET=exactly32characterslongsecret!!` → context loads
- Valid token generation + parsing round-trip

---

## Change 4: SEC-011 - OWASP Security Headers

### Files to Modify

| File | Change |
|------|--------|
| `src/main/java/.../config/SecurityConfig.java` | Add `http.headers(...)` in `filterChain()` |

### Implementation
```java
http.headers(headers -> headers
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(FrameOptionsConfig::deny)
    .referrerPolicy(ReferrerPolicyConfig::strictOriginWhenCrossOrigin)
    .permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=()"))
);
```

### Headers Added
| Header | Value | Purpose |
|--------|-------|---------|
| `X-Content-Type-Options` | `nosniff` | Prevent MIME sniffing |
| `X-Frame-Options` | `DENY` | Prevent clickjacking |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limit referrer info |
| `Permissions-Policy` | `geolocation=(), microphone=()` | Disable dangerous APIs |

### Tests

**Unit**: `SecurityConfigTest` (additional)
- `filterChain_headersConfigured_containsExpectedHeaders()`

**Integration**: `SecurityHeadersIntegrationTest`
- `GET /api/v1/health` → verify all 4 headers present in response
- `POST /api/v1/auth` → verify headers on error responses too
- `GET /actuator/health/liveness` → verify headers present

---

## Test Infrastructure

### Existing Test Dependencies (pom.xml)
- `spring-boot-starter-test`
- `spring-security-test`
- `mockito-junit-jupiter`
- `testcontainers` (if needed for integration)

### New Test Classes to Create
```
src/test/java/br/com/sprint1/challenge/config/
  ├── HmacSignatureFilterTest.java
  ├── SecurityConfigTest.java
  └── HmacSignatureFilterIntegrationTest.java

src/test/java/br/com/sprint1/challenge/service/impl/
  ├── JwtServiceImplTest.java
  └── JwtServiceIntegrationTest.java

src/test/java/br/com/sprint1/challenge/config/
  └── SecurityHeadersIntegrationTest.java
```

---

## Execution Order (TDD)

For each change:
1. **Write failing tests** (unit + integration)
2. **Run tests** → confirm RED
3. **Implement fix** in source
4. **Run tests** → confirm GREEN
5. **Refactor** if needed
6. **Commit** with message: `feat(security): <change description>`

### Suggested Sequence
1. **SEC-011 Security Headers** — Simplest, no config dependencies
2. **SEC-005 CORS Fail-Fast** — Pure validation, no external deps
3. **SEC-010 JWT Secret Validation** — Single class, clear boundary
4. **SEC-003 HMAC Default** — Touches config + filter, most complex

---

## Rollback Plan

Each change is independent. If any breaks:
- Revert single commit
- Config defaults can be overridden via env vars in production

---

## Verification Commands (Post-Implementation)

```bash
# 1. HMAC enabled by default, probes excluded
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health/liveness  # 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/user/me              # 401 (no HMAC)

# 2. CORS fail-fast
CORS_ALLOWED_ORIGINS="*" mvn spring-boot:run  # Should fail at startup

# 3. JWT secret validation
JWT_SECRET="short" mvn spring-boot:run  # Should fail at startup

# 4. Security headers
curl -I http://localhost:8080/api/v1/health | grep -E "X-Content-Type-Options|X-Frame-Options|Referrer-Policy|Permissions-Policy"
```

---

## Definition of Done

- [ ] All 4 changes implemented
- [ ] Unit tests pass (mvn test)
- [ ] Integration tests pass (mvn test)
- [ ] Full test suite passes (mvn clean test)
- [ ] Application starts with production-like config
- [ ] Verification commands all pass
- [ ] Changes committed with atomic commits per change