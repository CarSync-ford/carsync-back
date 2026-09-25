# Phase 2 - Auth Hardening (Semanas 2-3)

> HISTÓRICO — substituída como backlog desta entrega por [SEC-2026](sec-2026/README.md). Não executar cumulativamente. Checkboxes e alegações abaixo não comprovam o estado atual.

**Prioridade:** P1 - Alta | **Esforço:** ~2 semanas

Dependência: Migrações V8, V9 (pós V7).

---

## 2.1 Account Lockout / Brute-force Protection

### Migração V8: `V8__add_account_lockout_columns.sql`
```sql
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
CREATE INDEX idx_users_locked_until ON users(locked_until);
```

### `AuthServiceImpl` - Lógica
- Incrementar `failed_login_attempts` a cada falha
- Lock após 5 tentativas: setar `locked_until = now() + 15 minutos`
- No login: verificar se `locked_until > now()` → bloquear
- Login bem-sucedido: resetar `failed_login_attempts = 0`, `locked_until = null`
- Query para unlock automático (job ou no próprio login)

### `UserRepository`
```java
@Modifying
@Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
void unlockUser(@Param("id") Long id);
```

---

## 2.2 Refresh Token Rotation + Expiry

### Migração V9: `V9__add_refresh_token_expiry.sql`
```sql
ALTER TABLE users ADD COLUMN refresh_token_expires_at TIMESTAMP;
```

### `AuthServiceImpl` - Lógica
- No login: gerar refresh token + setar `refresh_token_expires_at = now() + 30 dias`
- No refresh (`POST /api/v1/auth/refresh`):
  - Validar token não expirado
  - **Rotacionar:** invalidar atual, gerar novo, atualizar expiry
  - Retornar novo access + refresh
- Na troca de senha (`POST /api/v1/auth/change-password`): revogar refresh token (setar expiry = now())

### Configuração: `application.yml`
```yaml
jwt:
  refresh-token-expiry-days: 30
```

---

## 2.3 Password Reset Flow

### Endpoints

#### `POST /api/v1/auth/forgot-password`
- Input: `{ "email": "user@domain.com" }`
- Gera JWT curto (15 min, single-use) com claim `purpose: PASSWORD_RESET`
- Envia email (mock ou integração real - ex: SendGrid, Azure Communication Services)
- Response: `202 Accepted` (sempre, para não vazar existência de email)

#### `POST /api/v1/auth/reset-password`
- Input: `{ "token": "jwt...", "newPassword": "..." }`
- Valida: token válido, não expirado, claim `purpose: PASSWORD_RESET`, single-use
- Atualiza senha (BCrypt)
- Revoga refresh token do usuário
- Marca token como usado (pode armazenar hash do token em tabela `password_reset_tokens`)

### Tabela opcional para single-use
```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT REFERENCES users(id),
    used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL
);
```

---

## 2.4 MFA Decision

### Opção A: Implementar TOTP (RFC 6238)
- Lib: `com.google.authenticator:google-authenticator:0.2.0` ou `dev.samstevens.totp:totp:1.1.0`
- Colunas já existem: `mfa_secret`, `mfa_enabled` em `users`
- Endpoints:
  - `POST /api/v1/auth/mfa/enable` → retorna QR code / secret
  - `POST /api/v1/auth/mfa/verify` → valida código, ativa MFA
  - `POST /api/v1/auth/mfa/disable` → desativa
- No login: se `mfa_enabled=true`, exigir código TOTP (2º fator)

### Opção B: Remover colunas se não usado
- Migração: `DROP COLUMN mfa_secret, mfa_enabled`
- Remover referências no código

### Decisão
Basear em requisito de negócio. Documentar ADR.

---

## Critério de Pronto Fase 2

- [ ] Lockout após 5 falhas, unlock automático 15 min
- [ ] Refresh token rotaciona a cada uso, expira em 30 dias
- [ ] Password reset flow end-to-end (token 15 min, single-use)
- [ ] MFA decidido e implementado/removido

---

## Notas

- **Testes:** Perfil `test` usa H2. Migrações V8-V9 precisam versões H2 em `src/test/resources/db/migration/h2/`
- **Front/Mobile:** Testar integração **após** Fase 2 (auth flow muda)