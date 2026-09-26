# Contratos de API, Autenticação e Autorização (02-api)

Data: 2026-09-26  
Status: Congelado para frentes 03-frontends e 05-observability  
Branch base: `origin/main` (`5f76776`)  

---

## 1. Contratos HTTP de Autenticação (`/api/v1/auth/**`)

### 1.1 Rotas Públicas (Sem necessidade de Bearer Token)

Apenas as seguintes operações sob `/api/v1/auth/**` são públicas:

| Método | Endpoint | Request Body | Response (Sucesso) | Erros Mapeados |
|---|---|---|---|---|
| `POST` | `/api/v1/auth` | `{"email": "...", "password": "..."}` | `200 OK`<br>`{"token": "...", "refreshToken": "..."}` | `400` Validação / formato<br>`401` Credenciais inválidas / conta bloqueada |
| `POST` | `/api/v1/auth/refresh` | `{"refreshToken": "..."}` | `200 OK`<br>`{"token": "...", "refreshToken": "..."}` | `400` Formato inválido<br>`401` Token expirado, adulterado, tipo incorreto ou revogado |
| `POST` | `/api/v1/auth/forgot-password` | `{"email": "..."}` | `202 Accepted` | `400` Formato de email inválido |
| `POST` | `/api/v1/auth/reset-password` | `{"token": "...", "newPassword": "..."}` | `204 No Content` | `400` Senha fora do padrão<br>`401` Token expirado, inválido ou usuário inexistente |

Demais rotas públicas da aplicação:
- `POST /api/v1/user` (cadastro de novos usuários)
- `GET /api/v1/health`
- `/actuator/health`, `/actuator/health/**`
- Documentação: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`

### 1.2 Rotas Autenticadas Sob `/api/v1/auth/**` (Exigem `Authorization: Bearer <access_token>`)

Todas as outras operações sob `/api/v1/auth/**` exigem autenticação válida:

| Método | Endpoint | Headers | Request Body | Response (Sucesso) | Erros Mapeados |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/change-password` | `Authorization: Bearer <token>` | `{"currentPassword": "...", "newPassword": "..."}` | `204 No Content` | `401` Token ausente/inválido ou senha atual incorreta<br>`400` Validação da nova senha |
| `POST` | `/api/v1/auth/mfa/enable` | `Authorization: Bearer <token>` | *vazio* | `200 OK`<br>`{"secret": "...", "qrCodeUri": "..."}` | `401` Não autenticado |
| `POST` | `/api/v1/auth/mfa/verify` | `Authorization: Bearer <token>` | `{"code": "..."}` | `204 No Content` | `401` Código inválido ou MFA não iniciado |
| `POST` | `/api/v1/auth/mfa/disable` | `Authorization: Bearer <token>` | *vazio* | `204 No Content` | `401` Não autenticado |
| `GET` | `/api/v1/auth/**` | `Authorization: Bearer <token>` | - | `401` se não autenticado / `404` se inexistente | `401` Não autenticado |

---

## 2. Alinhamento de Tipo de Principal (JWT Filter -> Controllers)

### Problema Anterior
- `JwtAuthenticationFilter` definia `SecurityContextHolder.getContext().setAuthentication(...)` com `claims.getSubject()` (um `java.lang.String` com o `userId`).
- Controllers (`AuthController`) utilizavam anotação `@AuthenticationPrincipal UserDetails userDetails`, recebendo `null` e disparando `NullPointerException` (HTTP 500) em endpoints autenticados como `enableMfa`, `verifyMfa`, `disableMfa`, `changePassword`.

### Contrato Atualizado
- O `JwtAuthenticationFilter` instancia e injeta um `org.springframework.security.core.userdetails.User` com:
  - `username`: UUID do usuário (`claims.getSubject()`)
  - `authorities`: `ROLE_<ROLE>` extraída de claim `role` (fallback: `ROLE_USER`)
  - `password`: `""`
- Controllers e serviços recebem com segurança `userDetails.getUsername()` para obter o ID do usuário autenticado.

---

## 3. Tratamento de Erros de Token (JJWT)

- Tokens de refresh malformados, com assinatura inválida, expirados ou com tipo incompatível resultam em:
  - **HTTP 401 Unauthorized** (com corpo estruturado `ApiErrorResponse`).
  - Anteriormente, falhas de parsing de token JJWT sem catch na camada de serviço ou exception handler resultavam em HTTP 500.
  - Implementado tratamento em `AuthServiceImpl` (lançando `TokenExpiredException` ou `InvalidTokenException`) e mapeamento centralizado de `ExpiredJwtException` e `JwtException` no `GlobalExceptionHandler`.

---

## 4. Rotação e Validade de Tokens (Refresh Token)

- **Capacidade do banco**: Colunas `refresh_token` em `USERS` e `USERS_AUD` possuem tamanho 1024 caracteres (`VARCHAR(1024)`).
- **Validade persistida**: `refresh_token_expires_at` persistido no banco com TTL configurável (padrão 30 dias em teste/produção).
- **Rotação estrita (Single-use)**: A cada uso de `POST /api/v1/auth/refresh`, um novo par `{token, refreshToken}` é emitido e o refresh token anterior é revogado no banco. Tentativas de replay do refresh token anterior resultam em `401 Unauthorized`.
- **Revogação em alteração de credencial**: `resetPassword` e `changePassword` revogam imediatamente o refresh token ativo do usuário (`revokeRefreshToken(userId)`).

---

## 5. Matriz de Perfis e Permissões (Bloqueio Documentado)

### Estado de Perfis
- O sistema possui suporte às roles `USER` e `ANALYST` (tabela `user_type` e JPA).
- **Bloqueio Formal**: A equivalência de domínio Ford entre os três perfis exigidos na rubrica (Brigadista, Gestor, Administrador) e os perfis do sistema (`USER`, `ANALYST`) permanece **pendente de definição do responsável docente**.
- Conforme instrução explícita de escopo, **nenhuma nova role foi criada** artificialmente sem matriz de domínio aprovada.
