# Eventos de Auditoria e Segurança da API (02-api)

Data: 2026-09-26  
Status: Produzido pela API para consumo da frente 05-observability  

---

## 1. Formato Estruturado de Log (JSON em Linha Única)

A API utiliza Logback com o layout customizado `br.com.sprint1.challenge.config.JsonLogLayout`. Todas as saídas de console são formatadas como **JSON válido em linha única** (`\n` apenas no terminador de registro).

### Campos Disponíveis

| Campo | Tipo | Descrição |
|---|---|---|
| `timestamp` | string (ISO-8601) | Timestamp exato do evento com timezone |
| `level` | string | Nível de log (`INFO`, `WARN`, `ERROR`, `DEBUG`) |
| `logger` | string | Classe emissora do log |
| `thread` | string | Nome da thread em execução |
| `message` | string | Mensagem descritiva do evento (sanitizada e com caracteres de escape JSON) |
| `exception` | string | Stack trace da exceção formatado em string única com escapes `\n` e `\t` (vazio se inexistente) |

---

## 2. Eventos de Auditoria de Segurança (`SECURITY_AUDIT`)

Os eventos de auditoria são emitidos na camada de serviço (`AuthServiceImpl`) **estritamente após o commit da transação de banco de dados** via `TransactionSynchronizationManager` e `TransactionSynchronization.afterCommit()`. Em caso de rollback da transação (falha ou exceção), **nenhum evento de auditoria é emitido**.

### Política de Privacidade e Sanitização (Zero PII e Segredos)
- **Não contém**: email, userId/UUID, CPF, senha em texto plano, hash de senha, token JWT, refresh token, secret TOTP/MFA ou código de verificação.
- **Campos padronizados**: `action:<AÇÃO> status:<STATUS>`

### Amostras Reais de Eventos de Auditoria

#### 2.1 Login Bem-sucedido
```json
{"timestamp":"2026-09-26T18:13:24.589-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:LOGIN status:SUCCESS","exception":""}
```

#### 2.2 Troca de Senha Concluída (`change-password`)
```json
{"timestamp":"2026-09-26T18:13:24.784-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:PASSWORD_CHANGE status:SUCCESS","exception":""}
```

#### 2.3 Redefinição de Senha Concluída (`reset-password`)
```json
{"timestamp":"2026-09-26T18:13:24.810-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:PASSWORD_RESET status:SUCCESS","exception":""}
```

#### 2.4 Habilitação de MFA Solicitada (`mfa/enable`)
```json
{"timestamp":"2026-09-26T18:13:24.638-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:MFA_ENABLE status:SUCCESS","exception":""}
```

#### 2.5 Verificação e Ativação de MFA Concluída (`mfa/verify`)
```json
{"timestamp":"2026-09-26T18:13:24.670-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:MFA_VERIFY status:SUCCESS","exception":""}
```

#### 2.6 Desativação de MFA (`mfa/disable`)
```json
{"timestamp":"2026-09-26T18:13:24.710-03:00","level":"INFO","logger":"br.com.sprint1.challenge.service.impl.AuthServiceImpl","thread":"main","message":"SECURITY_AUDIT action:MFA_DISABLE status:SUCCESS","exception":""}
```

---

## 3. Eventos de Violação e Falha de Segurança (`SECURITY_VIOLATION`)

Emitidos em nível `WARN` pelo `GlobalExceptionHandler` e pelos filtros de segurança (`JwtAuthenticationFilter`, `RateLimitFilter`). Incluem o IP do cliente (extraído de `X-Forwarded-For` ou socket remoto) para correlação e alertas de força bruta.

### Amostras Reais

#### 3.1 Falha de Autenticação (Credenciais Inválidas)
```json
{"timestamp":"2026-09-26T18:13:24.798-03:00","level":"WARN","logger":"br.com.sprint1.challenge.exception.GlobalExceptionHandler","thread":"main","message":"SECURITY_VIOLATION Auth Failed IP:192.168.1.50","exception":""}
```

#### 3.2 Conta Bloqueada por Excesso de Falhas (Lockout)
```json
{"timestamp":"2026-09-26T18:13:24.802-03:00","level":"WARN","logger":"br.com.sprint1.challenge.exception.GlobalExceptionHandler","thread":"main","message":"SECURITY_VIOLATION Account Locked IP:192.168.1.50","exception":""}
```

#### 3.3 Token JWT Expirado
```json
{"timestamp":"2026-09-26T18:13:24.821-03:00","level":"WARN","logger":"br.com.sprint1.challenge.exception.GlobalExceptionHandler","thread":"main","message":"SECURITY_VIOLATION Token Expired IP:127.0.0.1","exception":""}
```

#### 3.4 Token JWT Inválido ou Adulterado
```json
{"timestamp":"2026-09-26T18:13:24.976-03:00","level":"WARN","logger":"br.com.sprint1.challenge.exception.GlobalExceptionHandler","thread":"main","message":"SECURITY_VIOLATION Invalid Token IP:127.0.0.1","exception":""}
```

#### 3.5 Token JWT sem Subject / Claim Vazio
```json
{"timestamp":"2026-09-26T18:13:25.102-03:00","level":"WARN","logger":"br.com.sprint1.challenge.config.JwtAuthenticationFilter","thread":"main","message":"SECURITY_VIOLATION JWT Missing subject IP:127.0.0.1","exception":""}
```

---

## 4. Garantia de Máscara de Campos Sensíveis

O `JsonLogLayout` aplica regex automática sobre todas as mensagens formatadas para mascarar campos sensíveis (`password`, `senha`, `secret`, `token`, `hashed_password`, `cpf`), garantindo que eventuais logs acidentais de bibliotecas terceiras nunca exponham segredos em texto aberto.
