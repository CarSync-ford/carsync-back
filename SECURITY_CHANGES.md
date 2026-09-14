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

A borda externa do tráfego HTTPS é a **Cloudflare**, que termina TLS no
edge com certificado próprio e republica o domínio para o cliente final.
Em seguida, a Cloudflare abre uma segunda perna TLS até o Ingress do
**Azure Container Apps** em modo **Full** (cert do Azure, criptografado
mas sem validação estrita do certificado de origem); o ACA Ingress, por
sua vez, faz proxy reverso para o container, que expõe apenas a porta
`8080` em HTTP internamente (`Dockerfile` com `EXPOSE 8080`). O Ingress
do ACA está configurado para aceitar conexões **somente dos ranges
públicos da Cloudflare**, impedindo que o hostname
`*.azurecontainerapps.io` seja acessado diretamente, contornando todas as
camadas de borda. Do lado do Spring Boot, o que sustenta a cadeia é
`server.forward-headers-strategy: native` no `application.yml`, que faz
o Spring respeitar `X-Forwarded-Proto`/`X-Forwarded-For` injetados pelo
Ingress — gerando URLs absolutas com `https` e fazendo o Spring Security
considerar a requisição segura mesmo recebendo HTTP no socket. A cláusula
`requiresChannel().requiresSecure()` no `SecurityConfig` só é ativada
quando `server.ssl.enabled=true`, configuração reservada para ambientes
que terminam TLS dentro do próprio container (dev local, on-prem); em
produção ela permanece desligada porque seria redundante com Cloudflare
e Ingress, e provocaria loop de redirect. *Próximo passo opcional:*
migrar a Cloudflare de **Full** para **Full (Strict)** validando o cert
da origem do ACA — sem mudança no backend, basta acionar a chave no
painel da Cloudflare.

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

A defesa contra abuso e injeção é aplicada em **cascata**, com camadas
que se reforçam mutuamente:

- **Cloudflare** — proteção DDoS L3/L4 do plano Free e o **Free Managed
  Ruleset (OWASP)** ativo no Web Application Firewall, que filtra
  padrões conhecidos de SQL Injection, XSS, RCE, path traversal e demais
  itens do Top 10 antes de o tráfego atravessar o edge.
- **ACA Ingress** — borda autoritativa para CORS (restrito ao domínio do
  frontend) e *gatekeeper* da origem: como o Ingress só aceita conexões
  dos ranges da Cloudflare, qualquer requisição que chegue ao backend
  passou obrigatoriamente pelo WAF.
- **APIM (futuro)** — borda autoritativa para rate limiting de API
  quando entrar em operação.
- **Backend** — `RateLimitFilter` (Bucket4j) por IP e
  `CorsConfigurationSource` no Spring Security mantêm a defesa interna,
  garantindo que o serviço continue protegido em cenários de acesso
  interno, debug ou side-channel que escapem da borda.

Como há mais de uma camada com regras de origem, a variável
`CORS_ALLOWED_ORIGINS` precisa ser **espelhada** com a configuração do
Ingress do ACA: divergência entre as duas faz com que o efeito útil seja
sempre o da camada mais restritiva, podendo mascarar erros de
configuração de origem.

## Monitoramento

O `Dockerfile` baixa e injeta o agente Java do **Azure Application
Insights 3.5.4** via `-javaagent:/opt/agent.jar`, com a connection string
fornecida pela pipeline de deploy (`.github/workflows/deploy.yml` injeta
`APPLICATIONINSIGHTS_CONNECTION_STRING` como variável de ambiente). Os
canais de telemetria são complementares e não duplicados:

- O agente do App Insights faz auto-instrumentação e coleta **traces
  distribuídos, dependências (HTTP/JDBC) e métricas de runtime**, enviando
  para o recurso de Application Insights.
- O `stdout` do container, em formato JSON do `logback-spring.xml`, é
  capturado nativamente pelo ACA e enviado ao **Log Analytics Workspace**,
  ficando na tabela `ContainerAppConsoleLogs_CL` para queries KQL.
- Quando o Application Insights está em modo *workspace-based* (modo
  utilizado neste projeto), as duas pontas pousam no **mesmo** Log
  Analytics Workspace, permitindo correlacionar logs estruturados e traces
  na mesma query KQL.

O Spring Boot Actuator (`spring-boot-starter-actuator`) expõe os endpoints
de probes `/actuator/health/liveness` e `/actuator/health/readiness` na
porta `8080`, habilitados via `management.endpoint.health.probes.enabled=true`.
O readiness depende automaticamente do `DataSourceHealthIndicator` (via
`spring-boot-starter-data-jpa`), ou seja, falha de banco derruba o
readiness sem matar o container. Apenas o endpoint `health` é exposto via
HTTP (`management.endpoints.web.exposure.include=health`); demais endpoints
do Actuator ficam inacessíveis. O `SecurityConfig` libera explicitamente
`/actuator/health/liveness` e `/actuator/health/readiness` na allowlist
pública. Configuração das probes no ACA:
`livenessProbe.httpGet.path=/actuator/health/liveness`,
`readinessProbe.httpGet.path=/actuator/health/readiness`, porta `8080`.

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

## Gerenciamento de Segredos

Nenhum segredo é hardcoded no código ou commitado no repositório. Todo o
`application.yml` consome variáveis de ambiente via placeholders `${VAR}`
para os itens sensíveis: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `JWT_ISSUER`, `JWT_EXPIRATION_MINUTES`, `BCRYPT_SALT`,
`HMAC_SECRET`, `HMAC_ENABLED`, `CORS_ALLOWED_ORIGINS` e
`APPLICATIONINSIGHTS_CONNECTION_STRING`. Em produção, esses valores são
provisionados diretamente via **Azure Container Apps (ACA) Secrets** e
GitHub Actions Secrets (`.github/workflows/deploy.yml`), sendo injetados
como variáveis de ambiente no contêiner do ACA. Em desenvolvimento local,
o backend lê os mesmos placeholders a partir do arquivo `.env` (ignorado
pelo `.gitignore`); o `.env.example` versionado serve como template para
novos desenvolvedores. Essa estrutura desacopla o backend do mecanismo de
armazenamento dos segredos, mantendo o container agnóstico à origem das
variáveis.

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

**Estado atual:** os DTOs de saída são minimalistas (`GetUserResponse`,
`UserCreatedResponse`, etc.) e não retornam CPF nem e-mail nas respostas
autenticadas, e o `logback-spring.xml` mascara CPF, senha e token via regex
antes do log ser emitido. Não há, hoje, um pipeline sistemático de
mascaramento aplicado a entidades de negócio (`Customer`, `Lead`) — o que
é tolerável enquanto não existem usuários de análise consumindo a API.

**Plano de implementação (a executar quando o sistema de usuários de
análise for desenvolvido):**

1. **Nova role e migração de banco**
   - Adicionar a role `ANALYST` ao `user_type` via nova migração Flyway
     `V6__add_analyst_user_type.sql` (e equivalente em
     `src/test/resources/db/migration/h2/`).
   - Atualizar o `JwtServiceImpl`/`AuthServiceImpl` apenas para confirmar
     que a role já é gravada no claim `role` (já é) e propagada como
     `ROLE_ANALYST` pelo `JwtAuthenticationFilter`.

2. **Utilitário de mascaramento**
   - Criar `br.com.sprint1.challenge.util.DataMasker` com métodos puros e
     testáveis:
     - `maskCpf(String)` → `***.***.***-NN` (preserva os 2 últimos dígitos
       de verificação).
     - `maskEmail(String)` → `j***@d****.com` (preserva primeira letra do
       local-part e do domínio).
     - `maskPhone(String)` → `(**) ****-NNNN` (preserva os 4 últimos).
     - `maskFullName(String)` → primeiro nome + iniciais dos demais.
   - Cobertura unitária dedicada em `DataMaskerTest` com casos para nulos,
     vazios, formatos inválidos e fronteiras.

3. **DTOs/views dedicados para a role analítica**
   - Para cada entidade que expõe PII, criar um par de DTOs:
     `CustomerView` (para `ROLE_USER`/`ROLE_ADMIN`) e
     `CustomerAnalyticsView` (para `ROLE_ANALYST`), com os campos sensíveis
     já mascarados via `DataMasker` no mapper de saída.
   - Mantém o princípio "least privilege at the data layer": o JSON que
     trafega pela rede já chega anonimizado, não dependendo do cliente
     respeitar contrato algum.

4. **Endpoints isolados por role**
   - Expor um pacote de endpoints `/api/v1/analytics/**` anotados com
     `@PreAuthorize("hasRole('ANALYST')")`, que sempre retornam os DTOs
     anonimizados. Endpoints existentes em `/api/v1/customer360/**`,
     `/api/v1/leads/**` etc. continuam servindo `ROLE_USER`/`ROLE_ADMIN`
     com os DTOs originais.
   - Alternativa avaliada e descartada: `@JsonView` por role no mesmo
     endpoint — mais frágil porque qualquer rota nova precisa lembrar de
     anotar a view; endpoints separados deixam o contrato explícito.

5. **Testes de regressão**
   - Test de integração verificando que `GET /api/v1/analytics/customers`
     com token `ROLE_USER` retorna `403`.
   - Test de integração verificando que o mesmo endpoint com token
     `ROLE_ANALYST` retorna `200` com CPF, e-mail e telefone mascarados
     conforme as regras do `DataMasker`.
   - Test garantindo que o endpoint legado de `Customer` para `ROLE_USER`
     continua retornando os campos não mascarados.

6. **Logs e auditoria**
   - Acrescentar log `INFO ANALYTICS_ACCESS user:{} resource:{}` em cada
     endpoint analítico para rastrear quem acessou que conjunto de dados,
     reaproveitando o pipeline de logs JSON já existente. Combinado com
     Envers, fecha o ciclo de evidência LGPD: temos quem alterou
     (`revinfo`) e quem leu (logs).

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

**Estado atual:** não há rotina automatizada de hard delete nem de
ofuscação periódica. As tabelas críticas já têm os timestamps que servirão
de critério (`created_at`, `converted_at`, `last_login`), e a auditoria
via Envers continua preservando o histórico mesmo após qualquer remoção
lógica. O `@EnableScheduling` ainda não está habilitado.

**Plano de implementação (a executar junto com a introdução do `soft_delete`
nas entidades sensíveis):**

1. **Coluna `deleted_at` (soft delete) via Flyway**
   - Migração `V7__add_soft_delete_columns.sql` adicionando
     `deleted_at TIMESTAMP NULL` em `users`, `customers` e `leads` (com
     espelho em `src/test/resources/db/migration/h2/`).
   - Adicionar o campo correspondente nas entidades JPA
     (`@Column(name = "deleted_at") private LocalDateTime deletedAt;`) e
     decidir entre duas estratégias:
     - **Hibernate-driven:** anotar com `@SQLDelete` + `@SQLRestriction`
       para que `repository.delete(...)` faça `UPDATE ... SET deleted_at`
       e queries normais ignorem registros deletados. Menos código, mas
       acopla ao Hibernate.
     - **Service-driven (recomendado):** método explícito
       `softDelete(id)` em cada service, mantendo o `delete` físico para
       casos administrativos. Mais verboso, porém mais auditável.

2. **Habilitar agendamento**
   - Anotar `ArquiteturaOrientadaaServicosSprint1Application` com
     `@EnableScheduling`.
   - Criar `br.com.sprint1.challenge.service.DataRetentionService`
     concentrando os jobs.

3. **Job 1 — Hard delete de soft-deleted antigos**
   - Cron padrão `0 0 2 * * *` (diariamente às 02:00, baixa carga).
   - Pseudocódigo:
     ```java
     @Scheduled(cron = "${data-retention.cron-hard-delete}")
     public void purgeSoftDeleted() {
         var threshold = LocalDateTime.now()
             .minusDays(retentionProps.getSoftDeleteDays());
         int removed = userRepository.deleteByDeletedAtBefore(threshold)
                     + customerRepository.deleteByDeletedAtBefore(threshold)
                     + leadRepository.deleteByDeletedAtBefore(threshold);
         log.info("DATA_RETENTION hard_delete removed:{}", removed);
     }
     ```
   - Como cada entidade é `@Audited`, o Envers grava `revtype=2`
     (DELETE) automaticamente, então o histórico permanece em
     `users_aud`/`customers_aud`/`leads_aud` para fins de prova LGPD.

4. **Job 2 — Anonimização de PII em registros inativos**
   - Cron `0 0 3 * * SUN` (semanal, domingo de madrugada).
   - Para `User` com `last_login` anterior a N anos (config
     `data-retention.inactive-user-years`, default 5):
     - Substituir `username`, `email`, `cpf` por valores tokenizados
       (`anon_<uuid>@anon.local`, `00000000000`, etc.).
     - Manter `id` para preservar integridade referencial em FKs e
       tabelas de auditoria.
   - Para `Customer`/`Lead` sem atividade no mesmo período: aplicar a
     mesma rotina via `DataMasker` reutilizando o utilitário criado no
     plano de Anonimização.
   - Log: `INFO DATA_RETENTION anonymized entity:{} count:{}`.

5. **Configuração externa**
   - Bloco em `application.yml`:
     ```yaml
     data-retention:
       enabled: true
       soft-delete-days: 30
       inactive-user-years: 5
       cron-hard-delete: "0 0 2 * * *"
       cron-anonymize: "0 0 3 * * SUN"
     ```
   - Em ambiente de teste (`src/test/resources/application.yml`) manter
     `enabled: false` para não interferir nos testes existentes.
   - `@ConfigurationProperties(prefix = "data-retention")` em
     `DataRetentionProperties` para injeção tipada.

6. **Testes**
   - `DataRetentionServiceTest` com `Clock` injetado (substituir
     `LocalDateTime.now()` por `LocalDateTime.now(clock)`) para avançar
     o relógio nos testes e validar que o registro é removido/anonimizado
     exatamente após o threshold.
   - Test verificando que registros recentes (dentro do período de
     retenção) **não** são afetados.
   - Test confirmando que após o hard delete o `users_aud` ainda contém
     o histórico com `revtype=2`.

7. **Operacional**
   - Adicionar métrica customizada via Micrometer (`Counter
     "data_retention.removed"`) para visibilidade no Application Insights.
   - Documentar a política (dias de retenção e critérios) no README, já
     que é exigência de transparência da LGPD.

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

Esses eventos são emitidos no formato JSON estruturado do
`logback-spring.xml`, o que permite consultá-los diretamente no Log
Analytics via filtros por `level` e por substring `SECURITY_VIOLATION`.
Como o backend fica atrás de **três camadas de filtragem na borda** —
DDoS L3/L4 da Cloudflare, IP allowlist do ACA Ingress restrito aos ranges
da Cloudflare, e WAF Free Managed Ruleset (OWASP) da Cloudflare —, os
eventos `SECURITY_VIOLATION` que chegam a esses logs já passaram por
todos esses controles externos. Isso significa que o sinal é mais
"sintético": a frequência é menor (ataques triviais de SQLi, XSS e
floods volumétricos não chegam até aqui), porém cada evento registrado
representa uma anomalia que **escapou** do edge — o que aumenta a
relevância do alerta correspondente no Azure Monitor / Application
Insights e justifica thresholds KQL mais agressivos para essa categoria
de log.
