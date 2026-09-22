# Arquitetura Orientada a Servicos e Web Services

Projeto backend em Spring Boot (Java 21) com arquitetura orientada a servicos, exposicao de APIs REST e persistencia com JPA + H2 + Flyway.

## Visao geral

- Estilo de integracao: RESTful (rubrica: REST ou SOAP)
- Formatos suportados na API: JSON e XML
- Versionamento de rotas: `/api/v1`
- Contrato da API: Swagger/OpenAPI

## Arquitetura (componentes)

```text
Cliente/Front/Outro Sistema
		  |
		  v
Filtros de seguranca (camada transversal)
  - HmacSignatureFilter -> RateLimitFilter -> JwtAuthenticationFilter
		  |
		  v
Controllers (camada de apresentacao)
  - AuthController        - AnalyticsController
  - UserController         - AssistantController
  - HealthController       - ChurnController
                            - Customer360Controller
                            - LeadController
                            - StockController
		  |
		  v
Services (regras de negocio)
  - interfaces em service/
  - implementacoes em service/impl/
  - regras de churn (Strategy) em service/churnrule/
		  |
		  v
Repositories (acesso a dados com Spring Data JPA)
		  |
		  v
Banco H2 (schema controlado por Flyway)

Camadas transversais:
  - domain/    -> Value Objects e enums de dominio (Cpf, Email, RiskLevel)
  - exception/ -> GlobalExceptionHandler (respostas de erro padronizadas)
  - validation/-> Bean Validation customizada (@ValidCpf, @StrongPassword, @LowercaseEmail)
  - config/    -> SecurityConfig, JwtProperties, OpenApiConfig
```

Separacao de camadas no codigo:

- Apresentacao: `src/main/java/br/com/sprint1/challenge/controller`
- Seguranca/filtros: `src/main/java/br/com/sprint1/challenge/config` (`SecurityConfig`, `JwtAuthenticationFilter`, `HmacSignatureFilter`, `RateLimitFilter`)
- Negocio: `src/main/java/br/com/sprint1/challenge/service` e `src/main/java/br/com/sprint1/challenge/service/impl`
- Regras de churn (Strategy pattern): `src/main/java/br/com/sprint1/challenge/service/churnrule`
- Dados: `src/main/java/br/com/sprint1/challenge/repository`
- Entidades: `src/main/java/br/com/sprint1/challenge/entity`
- Dominio (Value Objects/enums): `src/main/java/br/com/sprint1/challenge/domain`
- Tratamento de erros: `src/main/java/br/com/sprint1/challenge/exception`

### Fluxo de comunicacao e autenticacao

1. Cliente faz `POST /api/v1/auth` com email/senha (JSON ou form-urlencoded).
2. `AuthController` -> `AuthServiceImpl` valida credenciais (BCrypt) e verifica bloqueio de conta (lockout apos 5 tentativas falhas, 15 min).
3. Login valido: `JwtServiceImpl` gera um access token (JWT, expira em minutos, claim de `role`) e um refresh token (expira em dias, rotacionado a cada uso). Resposta: `{ token, refreshToken }`.
4. Requisicoes seguintes enviam `Authorization: Bearer <token>` e passam pela cadeia de filtros antes de chegar no controller:
   `HmacSignatureFilter` (valida assinatura do payload) -> `RateLimitFilter` (limita requisicoes por IP) -> `JwtAuthenticationFilter` (valida assinatura/expiracao do JWT e popula o `SecurityContext`).
5. Autorizacao por papel: endpoints publicos (`/auth/**`, `POST /user`, Swagger, `/health`) nao exigem token; os demais exigem `authenticated()`, e alguns exigem role especifica via `@PreAuthorize` (ex.: `AnalyticsController` exige `ANALYST`).
6. Quando o access token expira, o cliente chama `POST /api/v1/auth/refresh` com o refresh token; `AuthServiceImpl` valida e rotaciona (emite novo access + refresh token, invalida o anterior).
7. MFA (opcional): `POST /auth/mfa/enable` gera um secret TOTP (RFC 6238); `POST /auth/mfa/verify` confere o codigo de 6 digitos antes de ativar.

### Principios de design aplicados (SOLID / Design Patterns / DDD / Clean Code)

- **Strategy + OCP/DIP**: `ChurnServiceImpl` nao calcula risco de churn sozinho — depende da abstracao `ChurnRiskRule` (`service/churnrule/`) e soma as contribuicoes de 4 regras concretas (`MileageRiskRule`, `WarrantyRiskRule`, `HealthStatusRiskRule`, `ServiceHistoryRiskRule`). Novas regras entram sem alterar `ChurnServiceImpl`.
- **SRP (Extract Class)**: a geracao/verificacao de codigo TOTP (RFC 6238) saiu de `AuthServiceImpl` para `TotpService`/`TotpServiceImpl` — autenticacao e MFA viraram responsabilidades separadas.
- **Facade**: `Customer360ServiceImpl` esconde a orquestracao entre `CustomerRepository`, `VehicleRepository`, `LeadRepository`, `DealershipRepository` e `ChurnService` atras de uma unica operacao (`getCustomer360`).
- **DDD - Value Objects**: `domain/Cpf.java` e `domain/Email.java` encapsulam as regras de validacao (digitos verificadores de CPF, formato de e-mail) que antes viviam soltas nos `ConstraintValidator`.
- **DDD - Entidade rica**: `Vehicle` ganhou comportamento proprio (`isHighMileage()`, `isUnderWarrantyEndingWithin()`, `hasCriticalHealth()`, `hasWarningHealth()`) em vez de expor so getters/setters.
- **Clean Code**: `Lead.status` deixou de ser `String` livre comparada por literal e virou o enum `LeadStatus` (`OPEN`/`CONVERTED`); `RiskLevel` substitui strings magicas ("ALTO"/"MEDIO"/"BAIXO") no calculo de churn; `JwtServiceImpl` eliminou a duplicacao de `Jwts.builder()...signWith(...)` repetida em 3 metodos, concentrando a montagem do token em `buildToken(...)`.

## Endpoints REST e metodos HTTP

Base path: `http://localhost:8080`

### Auth (publico)
- `POST /api/v1/auth` — login
- `POST /api/v1/auth/refresh` — renova access/refresh token
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/auth/change-password`
- `POST /api/v1/auth/mfa/enable` / `mfa/verify` / `mfa/disable`

### User
- `POST /api/v1/user` — cadastro (publico)
- `GET /api/v1/user/me` — dados do usuario autenticado (protegido, role `USER`)

### Health (publico)
- `GET /api/v1/health`

### Analytics
- `GET /api/v1/analytics/overview`
- `GET /api/v1/analytics/service-share`

### Vehicle Assistant
- `POST /api/v1/vehicle-assistant/interactions`
- `GET /api/v1/vehicle-assistant/interactions/{vehicleId}`

### Churn
- `GET /api/v1/churn/customers/{customerId}`
- `GET /api/v1/churn/risk-list`
- `POST /api/v1/churn/customers/{customerId}/recalculate`

### Customer 360
- `GET /api/v1/customers/{customerId}/360`

### Leads
- `GET /api/v1/leads`
- `GET /api/v1/leads/{id}`
- `POST /api/v1/leads`
- `POST /api/v1/leads/{id}/convert`

### Stock
- `GET /api/v1/stock/alerts`

## Contrato da API (Swagger/OpenAPI)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Configuracao OpenAPI: `src/main/java/br/com/sprint1/challenge/config/OpenApiConfig.java`

## Tratamento de erros e boas praticas

- Handler global de excecoes: `src/main/java/br/com/sprint1/challenge/exception/GlobalExceptionHandler.java`
- Respostas padronizadas para 400, 404 e 500
- Validacao de entrada com Bean Validation (`@Valid` nos endpoints)
- API versionada (`/api/v1`) para evolucao segura

## Banco de dados e migracoes

- Configuracao de datasource e JPA: `src/main/resources/application.yml`
- Banco para execucao local: H2 em memoria
- Migracao de schema com Flyway: `src/main/resources/db/migration/V1__create_schema.sql`

## Como executar

```bash
cd /home/hellen/Downloads/ArquiteturaOrientadaaServicos_Sprint1
mvn clean test
mvn spring-boot:run
```

## Como validar rapidamente

```bash
curl -s http://localhost:8080/v3/api-docs | head
curl -s http://localhost:8080/api/v1/churn/risk-list
```

## Evidencias para a rubricagem da sprint

- Integracao por Web Services: API REST implementada em `controller/` com contrato OpenAPI
- SOA: organizacao modular em controllers, services e repositories
- Padroes e boas praticas: REST, JSON/XML, validacao e exception handling centralizado
- Banco: configuracao de conexao no `application.yml` e migracao versionada com Flyway

## Alunos 3ESR
- 558385 - Alexia Ramalho
- 557943 - Enzo Real
- 555454 - Gustavo Pasquini
- 559008 - Hellen Silva
- 557397 - Lorenzo Acquesta
