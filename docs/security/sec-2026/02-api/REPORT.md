# Relatório Técnico de Hardening da API (02-api)

Data: 2026-09-26  
Ambiente: Worktree isolado `agent-a33762218b047253f` baseado em `origin/main 5f76776`  
Autor: Frente 02-api  
Status da Suite: **222 testes executados, 0 falhas, 0 erros** (`mvn clean test`)  

---

## 1. Resumo Executivo

A frente 02-api executou o plano de hardening de segurança da API do CarSync, cobrindo:
1. **Restrição estrita de endpoints públicos de autenticação** em `SecurityConfig`.
2. **Correção da injeção do principal de segurança** no `JwtAuthenticationFilter`, eliminando `NullPointerException` (HTTP 500) em controllers que esperam `@AuthenticationPrincipal UserDetails`.
3. **Tratamento de exceções JJWT** para retornar HTTP 401 Unauthorized em vez de HTTP 500 para tokens expirados ou adulterados.
4. **Logs estruturados em JSON válido de linha única** no Logback, com escape seguro de caracteres e máscara automática de campos sensíveis (senhas, CPF, tokens).
5. **Auditoria de segurança pós-commit** em `AuthServiceImpl`, garantindo que eventos de auditoria só são emitidos quando a transação bancária é confirmada com sucesso (zero logs em rollback).
6. **Esquema de autenticação OpenAPI (`bearerAuth`)** adicionado à documentação `/v3/api-docs`.
7. **Diagnóstico da causa-raiz do Swagger UI 404** sem alteração indevida de dependências no `pom.xml`.

---

## 2. Detalhamento das Alterações de Hardening

### 2.1 Restrição de Acesso em `SecurityConfig`
- A regra `.requestMatchers("/api/v1/auth/**").permitAll()` expunha rotas autenticadas (`POST /api/v1/auth/change-password`, `POST /api/v1/auth/mfa/**`, etc.) ao acesso sem token Bearer.
- Restringidas as rotas públicas de `/api/v1/auth/**` estritamente a `POST /api/v1/auth`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/forgot-password` e `POST /api/v1/auth/reset-password`.
- Todas as demais rotas exigem autenticação (`.anyRequest().authenticated()`). Chamadas sem JWT retornam `401 Unauthorized`.

### 2.2 Alinhamento do Principal no `JwtAuthenticationFilter`
- O filtro criava o token de autenticação passando o subject `String` diretamente como principal. O `AuthController` injetava `@AuthenticationPrincipal UserDetails userDetails`, recebendo `null` e disparando NPE ao chamar `userDetails.getUsername()`.
- O filtro agora instancia `org.springframework.security.core.userdetails.User` com username, senha vazia e authorities `ROLE_<role>`, permitindo injeção correta de `UserDetails`.
- Adicionado log de violação de segurança quando o token JWT não contém subject válido (`claims.getSubject() == null || isBlank()`).

### 2.3 Tratamento de Exceções JJWT no Refresh e Reset de Senha
- Em `AuthServiceImpl`, blocos de parsing de token JJWT em `refreshToken` e `resetPassword` tratam `ExpiredJwtException` lançando `TokenExpiredException` e qualquer outra `JwtException` lançando `InvalidTokenException`.
- `GlobalExceptionHandler` mapeia `ExpiredJwtException` e `JwtException` para status `401 Unauthorized` com corpo estruturado `ApiErrorResponse`.
- Adicionado também handler para `HttpMediaTypeNotSupportedException` (retornando `415 Unsupported Media Type`).

### 2.4 Logs Estruturados em JSON Válido (`JsonLogLayout` e `logback-spring.xml`)
- Criada classe `br.com.sprint1.challenge.config.JsonLogLayout` estendendo `ch.qos.logback.core.LayoutBase<ILoggingEvent>`.
- Implementado escape nativo de caracteres (`"`, `\`, `\n`, `\r`, `\t` e caracteres de controle).
- Stack traces são convertidos em string contínua devidamente escapada via `ThrowableProxyUtil.asString(tp)`.
- Regex de mascaramento substitui campos sensíveis (`password`, `senha`, `secret`, `token`, `hashed_password`, `cpf`) por `***`.
- Configurado `logback-spring.xml` com `LayoutWrappingEncoder`.

### 2.5 Eventos de Auditoria Pós-Commit
- Implementado método `logAfterCommit(String message)` em `AuthServiceImpl` utilizando `TransactionSynchronizationManager` e `TransactionSynchronization.afterCommit()`.
- Se a transação sofrer rollback ou abortar por exceção, o log de auditoria não é executado.
- Eventos sanitizados sem PII (sem email, CPF, userId) e sem segredos: `action:LOGIN`, `action:PASSWORD_RESET`, `action:PASSWORD_CHANGE`, `action:MFA_ENABLE`, `action:MFA_VERIFY`, `action:MFA_DISABLE`.

### 2.6 OpenAPI Bearer Authentication Scheme
- Declarado `SecurityScheme` do tipo `HTTP`, scheme `bearer`, bearerFormat `JWT` nos componentes de `OpenApiConfig.java`.
- O endpoint `/v3/api-docs` agora expõe a definição de autenticação bearer consumível por ferramentas e clientes.

---

## 3. Diagnóstico da Causa-Raiz do Swagger UI 404

### Sintoma
- `/v3/api-docs` responde perfeitamente com status `200 OK` e especificação OpenAPI completa.
- `/swagger-ui.html` responde com `404 Not Found` (`NoResourceFoundException`).

### Causa-Raiz
1. O projeto utiliza `spring-boot-starter-parent:4.1.1` (Spring Framework 7.4.x).
2. O Spring Framework 7 reestruturou o roteamento e localização de recursos estáticos do Spring WebMvc.
3. As bibliotecas `springdoc-openapi-starter-webmvc-ui:3.1.1` e `org.webjars:swagger-ui:5.32.15` dependem de resoluções de caminho do Spring Framework 6 / Spring Boot 3.x.
4. Quando a requisição `/swagger-ui.html` tenta acessar `/swagger-ui/index.html` ou arquivos estáticos do webjar, o `ResourceHttpRequestHandler` do Spring 7 lança `NoResourceFoundException: No static resource swagger-ui/index.html`, que é capturado pelo `GlobalExceptionHandler` e retornado como 404.
5. Conforme restrição de escopo da frente 02, o arquivo `pom.xml` não foi alterado.

---

## 4. Evidência de Testes

Execução da suite completa:
`mvn clean test`
**Resultado**: `Tests run: 222, Failures: 0, Errors: 0, Skipped: 0`
