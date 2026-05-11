# Spec: POST /api/v1/user — Criação de Usuário

**Data:** 11/05/2026  
**Autor:** Enzo  
**Branch:** `feat/post-user`

---

## 1. Objetivo

Implementar endpoint de criação de usuário via POST com validação completa de payload (CPF, email, senha forte), verificação de unicidade de CPF, hash de senha com bcrypt e inicialização de campos MFA como falso. Retorna o ID (UUIDv4) do usuário criado.

---

## 2. Alterações no Banco de Dados

**Arquivo de migração:** `src/main/resources/db/migration/V2__create_user_tables.sql`

```sql
CREATE TABLE user_type (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    cpf VARCHAR(255) NOT NULL UNIQUE,
    last_login TIMESTAMP,
    user_type VARCHAR(255) NOT NULL,
    mfa_secret VARCHAR(255),
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_token VARCHAR(255),
    CONSTRAINT fk_users_user_type FOREIGN KEY (user_type) REFERENCES user_type(id)
);

INSERT INTO user_type (id, type) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'USER');
```

**Entidade JPA:** `entity/UserType.java`
- Campos: `id` (String), `type` (String)

**Entidade JPA:** `entity/User.java`
- Campos: `id` (String), `username` (String), `email` (String), `hashedPassword` (String), `cpf` (String), `lastLogin` (LocalDateTime), `mfaSecret` (String), `mfaEnabled` (Boolean), `refreshToken` (String)
- Relacionamentos: `@ManyToOne` para `UserType` via coluna `user_type`

---

## 3. Contrato da API

| Método | Path | Descrição | Status |
|--------|------|-----------|--------|
| POST | `/api/v1/user` | Criar novo usuário | 201 |

**Content-Types:** `application/json`, `application/xml`

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "cpf": "string"
}
```

**Response Body (201):**
```json
{
  "id": "uuid-v4-string"
}
```

**Response Body (400):**
```json
{
  "timestamp": "2026-05-11T18:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação nos dados enviados.",
  "path": "/api/v1/user",
  "details": ["campo: mensagem de erro"]
}
```

---

## 4. DTOs

**Arquivo:** `dto/UserDtos.java`

```java
public final class UserDtos {
    private UserDtos() {}

    public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @StrongPassword String password,
        @NotBlank @ValidCpf String cpf
    ) {}

    public record UserCreatedResponse(
        String id
    ) {}
}
```

---

## 5. Regras de Negócio

1. CPF deve ser validado pelo algoritmo de dígitos verificadores antes de qualquer processamento
2. CPF deve ser único no banco — se já existir, retornar 400 com mensagem de erro
3. Senha deve ter no mínimo 8 caracteres, pelo menos 1 letra maiúscula e pelo menos 1 caractere especial
4. Email deve ser um formato válido
5. Senha deve ser hasheada com bcrypt usando salt configurado via variável de ambiente `BCRYPT_SALT`
6. `user_type` deve ser atribuído automaticamente como "USER" (tipo padrão)
7. `mfa_enabled` deve ser `false`, `mfa_secret`, `last_login` e `refresh_token` devem ser `null`
8. ID do usuário deve ser gerado como UUIDv4 pelo sistema

**Exceções esperadas:**
- `DuplicateCpfException` quando CPF já existe no banco → 400
- `MethodArgumentNotValidException` quando Bean Validation falha → 400

---

## 6. Testes

**Arquivo:** `src/test/java/br/com/sprint1/challenge/UserTests.java`

### Abordagem TDD — testes escritos ANTES da implementação

**Testes unitários de validação (`validation/CpfValidatorTest.java`):**
- [ ] CPF válido retorna true
- [ ] CPF com dígitos verificadores errados retorna false
- [ ] CPF com todos dígitos iguais retorna false
- [ ] CPF com tamanho incorreto retorna false
- [ ] CPF nulo/vazio retorna false

**Testes unitários de validação (`validation/PasswordValidatorTest.java`):**
- [ ] Senha válida (≥8 chars, maiúscula, especial) retorna true
- [ ] Senha < 8 caracteres retorna false
- [ ] Senha sem letra maiúscula retorna false
- [ ] Senha sem caractere especial retorna false
- [ ] Senha nula/vazia retorna false

**Testes unitários do service (`service/UserServiceTest.java`):**
- [ ] Criação com sucesso: CPF novo → salva, retorna UUID, senha é hash, mfa_enabled=false
- [ ] CPF duplicado → lança DuplicateCpfException
- [ ] Senha armazenada não é plaintext
- [ ] user_type padrão "USER" é atribuído

**Testes de integração (`controller/UserControllerTest.java`):**
- [ ] POST payload válido → 201 + body com id (formato UUID)
- [ ] POST CPF duplicado → 400
- [ ] POST CPF inválido (dígitos errados) → 400
- [ ] POST email inválido → 400
- [ ] POST senha < 8 chars → 400
- [ ] POST senha sem maiúscula → 400
- [ ] POST senha sem caractere especial → 400
- [ ] POST campos obrigatórios ausentes → 400
- [ ] Verificar no banco: mfa_enabled = false após criação
- [ ] Verificar no banco: senha armazenada é hash

---

## 7. Critérios de Aceite

- [ ] Migração Flyway V2 executa sem erros
- [ ] Endpoint responde em JSON e XML
- [ ] Validação com Bean Validation + custom validators funciona
- [ ] Erros retornam no formato `ApiErrorResponse`
- [ ] `mvn clean test` passa sem falhas
- [ ] Swagger documenta o novo endpoint
- [ ] Testes escritos antes da implementação (TDD)

---

## 8. Task Breakdown (TDD)

**Task 1:** Infraestrutura — migração Flyway V2, dependência jbcrypt, configuração bcrypt.salt

**Task 2:** Escrever testes unitários para validação de CPF (RED)

**Task 3:** Implementar validador de CPF (GREEN)

**Task 4:** Escrever testes unitários para validação de senha (RED)

**Task 5:** Implementar validador de senha (GREEN)

**Task 6:** Escrever testes unitários para UserService (RED) + criar stubs mínimos (interfaces, DTOs, entidades, repositórios, exception)

**Task 7:** Implementar entidades, repositórios, DTOs e UserServiceImpl (GREEN)

**Task 8:** Escrever testes de integração para o endpoint (RED)

**Task 9:** Implementar UserController e wiring final (GREEN)
