# Alterações de Segurança e Arquitetura no Backend

Este documento descreve, requisito a requisito, as alterações realizadas no
backend Spring Boot deste projeto para atender à matriz de segurança proposta
(infraestrutura, fundações, identidade, defesa e regras avançadas). Cada
seção descreve, em até um parágrafo, a ação tomada e onde ela está
implementada no código.

## Criptografia em Repouso

Tratado fora do backend, no nível de infraestrutura (banco gerenciado e
volumes do cluster). A aplicação não armazena dados sensíveis em arquivos
locais; toda persistência ocorre via JPA/Flyway no PostgreSQL gerenciado
(`spring.datasource.url` em `src/main/resources/application.yml`), cuja
criptografia em repouso é provida pelo serviço de nuvem. No nível de
aplicação, foi reforçado o hashing de credenciais com BCrypt
(`UserServiceImpl` / `AuthServiceImpl`) para que mesmo o conteúdo persistido
já chegue cifrado/derivado ao banco.

## Forçar HTTPS / TLS

A terminação TLS é feita pelo Ingress da nuvem; o container expõe apenas a
porta 8080 internamente (ver `Dockerfile` com `EXPOSE 8080`). Para suportar
o repasse correto dos cabeçalhos `X-Forwarded-*`, foi habilitado
`server.forward-headers-strategy: native` no `application.yml`, e o
`SecurityConfig` aplica `requiresChannel().requiresSecure()` quando
`server.ssl.enabled=true`, permitindo forçar HTTPS quando a aplicação estiver
em ambientes que terminam TLS no próprio container.

## Rate Limiting e CORS

Foi adicionado o `RateLimitFilter`
(`src/main/java/br/com/sprint1/challenge/config/RateLimitFilter.java`),
implementado com **Bucket4j** (10 requisições/segundo por IP, com `Refill`
greedy), registrado na cadeia do Spring Security antes do
`UsernamePasswordAuthenticationFilter`. O CORS é configurado de forma
centralizada no `SecurityConfig` via `CorsConfigurationSource`, lendo as
origens permitidas da propriedade `app.cors.allowed-origins` (variável
`CORS_ALLOWED_ORIGINS`), restringindo métodos a `GET/POST/PUT/DELETE/OPTIONS`
e mantendo `allowCredentials=true`.

## Monitoramento

O `Dockerfile` baixa e injeta o agente Java do **Azure Application
Insights 3.5.4** via `-javaagent:/opt/agent.jar`, com a connection string
fornecida pela pipeline de deploy (`.github/workflows/deploy.yml` injeta
`APPLICATIONINSIGHTS_CONNECTION_STRING` como variável de ambiente). Há ainda
um `HealthController` em `/api/v1/health` retornando `status: healthy`, e o
`SecurityConfig` libera `/actuator/health` na allowlist para futura ativação
do Spring Boot Actuator (a dependência `spring-boot-starter-actuator` ainda
não está adicionada ao `pom.xml` — pendente caso se queira expor
liveness/readiness padrão).

## Limitação de Payload

Configurado em `src/main/resources/application.yml` na seção
`spring.servlet.multipart` com `max-file-size: 1MB` e
`max-request-size: 1MB`. O `GlobalExceptionHandler` trata
`MaxUploadSizeExceededException` e devolve `413 Payload Too Large` com a
mensagem padronizada "O tamanho do arquivo excede o limite permitido de 1MB",
sem expor stack trace.

## Tratamento de Erros

Implementado `GlobalExceptionHandler` anotado com `@RestControllerAdvice`
(`src/main/java/br/com/sprint1/challenge/exception/GlobalExceptionHandler.java`),
mapeando exceções específicas (`ResourceNotFoundException`,
`DuplicateCpfException`, `DuplicateEmailException`,
`InvalidCredentialsException`, `MethodArgumentNotValidException`,
`ConstraintViolationException`, `NoResourceFoundException`,
`MaxUploadSizeExceededException` e `Exception` genérica) para o DTO
`ApiErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`,
`details`). Stack traces nunca são serializadas na resposta; apenas
mensagens curtas e a lista de erros de validação por campo.

## Logs Estruturados

Adicionado `src/main/resources/logback-spring.xml` com appender
`JSON_CONSOLE` que emite cada linha de log em JSON
(`timestamp`, `level`, `logger`, `thread`, `message`, `exception`). O padrão
inclui um filtro `%replace` com regex que mascara automaticamente valores
de campos sensíveis no `message` — `password`, `senha`, `secret`, `token`,
`hashed_password`, `hashedPassword` e `cpf` — substituindo o valor por
`***` antes da serialização, atendendo aos requisitos de LGPD nos logs.

## Autenticação JWT

Implementada com `spring-boot-starter-security` + biblioteca
`io.jsonwebtoken (jjwt 0.12.6)`. A emissão fica em `JwtServiceImpl`
(HS256, chave de pelo menos 256 bits via `Keys.hmacShaKeyFor`, com
`subject`, `email`, `role`, `issuer`, `iat`, `exp`), parametrizada por
`JwtProperties` (`jwt.secret`, `jwt.expiration-minutes`, `jwt.issuer`). A
validação é feita pelo `JwtAuthenticationFilter`, registrado no
`SecurityConfig` antes do `UsernamePasswordAuthenticationFilter`, que extrai
o `Bearer` do header `Authorization`, valida a assinatura/expiração e
popula o `SecurityContext` com `UsernamePasswordAuthenticationToken` e
authority `ROLE_<role>`. O endpoint `POST /api/v1/auth` (`AuthController`)
faz o login e retorna o token; o `AuthServiceImpl` aplica BCrypt + dummy
hash anti-timing para evitar enumeração de usuários.

## Controle de Acesso (RBAC)

A configuração `@EnableMethodSecurity` está ativa no `SecurityConfig`,
permitindo o uso de `@PreAuthorize` nos controllers. O exemplo concreto é
o endpoint `GET /api/v1/user/me` em `UserController`, anotado com
`@PreAuthorize("hasRole('USER')")`, que só executa após a autenticação
JWT popular as authorities. As demais rotas críticas podem ser protegidas
pelo mesmo padrão (`@PreAuthorize("hasRole('ADMIN')")`, etc.) reaproveitando
a role gravada no token.

## Trilha de Auditoria

Adicionada a dependência `org.hibernate.orm:hibernate-envers` ao `pom.xml`
e as entidades críticas — `User`, `UserType`, `Customer` e `Lead` — foram
anotadas com `@Audited`. As tabelas de auditoria (`users_aud`,
`user_type_aud`, `customers_aud`, `leads_aud` e `revinfo`) foram criadas
via Flyway nas migrações `V4__add_envers_audit_tables.sql` e
`V5__add_revinfo_sequence.sql` (com a versão equivalente em
`src/test/resources/db/migration/h2/` para o perfil de testes), garantindo
que toda criação/alteração/remoção dessas entidades fique registrada com
o `rev` e o `revtype` para fins de rastreabilidade e LGPD.

## Validação e Normalização

Os DTOs de entrada usam `spring-boot-starter-validation` (Jakarta Bean
Validation). `AuthDtos.AuthRequest` aplica `@NotBlank`, `@Email`,
`@LowercaseEmail` e `@Size(min=6,max=20)`; `UserDtos.CreateUserRequest`
adiciona `@StrongPassword` (validador customizado em
`PasswordValidator` exigindo 8+ caracteres, maiúscula e símbolo) e
`@ValidCpf` (validador completo com cálculo dos dígitos verificadores em
`CpfValidator`). A normalização de e-mail para minúsculas é forçada pelo
`LowercaseEmailValidator`, evitando duplicação por diferença de caixa.

## Sanitização

Toda a camada de acesso a dados usa **Spring Data JPA** com
`JpaRepository` (`UserRepository`, `LeadRepository`, etc.) e queries
parametrizadas com `@Param` (ex.: `updateLastLoginById` em
`UserRepository`), eliminando concatenação de SQL e bloqueando SQL
Injection nativamente. Não há uso de `Statement` puro nem
`createNativeQuery` com interpolação de strings. Para textos livres
(descrição de leads, etc.) o conteúdo é serializado via Jackson, que faz
escape automático de caracteres especiais ao gerar JSON.

## Anonimização

A anonimização sistemática nas respostas ainda **não está implementada**
no nível dos DTOs/mappers — os DTOs atuais (`GetUserResponse`,
`UserCreatedResponse`, etc.) já são minimalistas e não retornam CPF nem
e-mail nas respostas autenticadas, o que reduz a superfície de exposição.
A camada de logs já mascara CPF, senha e token via regex no
`logback-spring.xml`. Próximo passo planejado: aplicar lógica explícita
de mascaramento (ex.: `***.***.***-NN`) em mappers de saída para qualquer
endpoint futuro que precise expor dados pessoais.

## Assinatura de Payloads

Implementado `HmacSignatureFilter`
(`src/main/java/br/com/sprint1/challenge/config/HmacSignatureFilter.java`),
registrado antes dos demais filtros na cadeia do Spring Security. Ele lê
o header `X-HMAC-Signature`, recalcula um HMAC-SHA256 sobre o corpo da
requisição (usando `CachedBodyHttpServletRequest` para permitir leitura
posterior pelo controller) ou sobre `URI + query string` quando não há
body, e compara em tempo constante com `MessageDigest.isEqual`. O segredo
e a ativação são controlados por `hmac.secret` e `hmac.enabled`
(variáveis `HMAC_SECRET` e `HMAC_ENABLED`); requisições sem assinatura
ou com assinatura inválida recebem `401`.

## Descarte Seguro

Pendente de implementação no backend. A rotina automatizada de hard
delete/ofuscação por `@Scheduled` ainda não está presente no código (o
projeto ainda não habilita `@EnableScheduling` nem possui jobs
periódicos). A base já está pronta para receber essa rotina graças à
auditoria via Envers (que preserva o histórico mesmo após a remoção
lógica) e às tabelas com colunas de timestamp (`created_at`,
`converted_at`, `last_login`), que servirão como critério de elegibilidade
para o descarte conforme a política LGPD.

## Monitoramento de Anomalias

Foram adicionados logs customizados em pontos sensíveis com prefixo
padronizado `SECURITY_VIOLATION` para facilitar alertas e dashboards no
Application Insights:
- `RateLimitFilter` registra `WARN` `SECURITY_VIOLATION Rate Limit Exceeded
  IP:{}` quando o bucket é estourado.
- `JwtAuthenticationFilter` registra `WARN` `SECURITY_VIOLATION JWT Invalid
  IP:{} reason:{}` em falhas de validação de token.
- `GlobalExceptionHandler` registra `WARN` `SECURITY_VIOLATION Auth Failed
  IP:{}` em `InvalidCredentialsException` e `ERROR` na exceção genérica.

Esses eventos são emitidos no formato JSON estruturado do `logback-spring.xml`,
o que permite consultá-los diretamente no Log Analytics via filtros por
`level` e por substring `SECURITY_VIOLATION`.
