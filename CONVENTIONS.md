# Project Conventions & Architecture

## 1. Tech Stack & Environment
- **Backend:** Java 21 com Spring Boot.
- **Build Tool:** Maven. 
- **Database:** H2 (In-memory para desenvolvimento) com migrações via Flyway.

## 2. Architecture Rules (STRICT)
O projeto segue uma arquitetura em camadas rígida. Ao criar ou modificar funcionalidades, você DEVE respeitar o seguinte fluxo:
- **Presentation Layer (Controllers):** `src/main/java/br/com/sprint1/challenge/controller`. Devem apenas receber requisições, delegar para os Services e retornar respostas. NUNCA coloque regra de negócio aqui.
- **Business Logic (Services):** Interfaces em `service` e implementações em `service/impl`. Toda a lógica de negócio (como os cálculos de Churn e Customer 360) reside aqui.
- **Data Access (Repositories):** `src/main/java/br/com/sprint1/challenge/repository`. Use Spring Data JPA.
- **Data Transfer Objects (DTOs):** NUNCA retorne Entidades JPA diretamente nos Controllers. Sempre mapeie Entidades para DTOs (`src/main/java/br/com/sprint1/challenge/dto`).

## 3. API Guidelines
- **Versioning:** Todos os novos endpoints DEVEM estar sob o path `/api/v1/`.
- **Documentation:** Mantenha as anotações do OpenAPI/Swagger atualizadas nos Controllers.
- **Validation:** Utilize Bean Validation (`@Valid`, `@NotNull`, etc.) nos DTOs de entrada.
- **Error Handling:** Utilize o Global Exception Handler do projeto. Não retorne strings de erro genéricas.

## 4. Development Commands for AI
Se precisar compilar ou rodar testes para validar o plano antes de finalizar, utilize:
- Build & Test global: `mvn clean test`
- Teste isolado: `mvn test -Dtest=ClassName#methodName`

## 5. SDD Execution Constraints
- Ao implementar uma Spec, não altere o esquema do banco de dados (Flyway `V1__create_schema.sql`) a menos que a Spec explicitamente exija.
- Siga estritamente o padrão Conventional Commits em inglês para todas as alterações.