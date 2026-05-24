# Arquitetura Orientada a Servicos e Web Services - Sprint 1

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
Controllers (camada de apresentacao)
  - AnalyticsController
  - AssistantController
  - ChurnController
  - Customer360Controller
  - LeadController
  - StockController
		  |
		  v
Services (regras de negocio)
  - interfaces em service/
  - implementacoes em service/impl/
		  |
		  v
Repositories (acesso a dados com Spring Data JPA)
		  |
		  v
Banco H2 (schema controlado por Flyway)
```

Separacao de camadas no codigo:

- Apresentacao: `src/main/java/br/com/sprint1/challenge/controller`
- Negocio: `src/main/java/br/com/sprint1/challenge/service` e `src/main/java/br/com/sprint1/challenge/service/impl`
- Dados: `src/main/java/br/com/sprint1/challenge/repository`
- Entidades: `src/main/java/br/com/sprint1/challenge/entity`

## Endpoints REST e metodos HTTP

Base path: `http://localhost:8080`

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
558385 - Alexia Ramalho
557943 - Enzo Real
555454 - Gustavo Pasquini
559008 - Hellen Silva
557397 - Lorenzo Acquesta
