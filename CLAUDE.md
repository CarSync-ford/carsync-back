# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot backend application (Java 21) implementing a service-oriented architecture for a car dealership management system. The application exposes RESTful APIs with JSON and XML support, uses JPA with H2 database and Flyway for schema migration, and follows clean separation of concerns with controllers, services, and repositories.

## Architecture and Structure

The application follows a layered architecture with clear separation of concerns:

- **Presentation Layer**: Controllers in `src/main/java/br/com/sprint1/challenge/controller`
- **Business Logic Layer**: Services in `src/main/java/br/com/sprint1/challenge/service` and implementations in `src/main/java/br/com/sprint1/challenge/service/impl`
- **Data Access Layer**: Repositories in `src/main/java/br/com/sprint1/challenge/repository`
- **Entities**: Domain objects in `src/main/java/br/com/sprint1/challenge/entity`
- **DTOs**: Data Transfer Objects in `src/main/java/br/com/sprint1/challenge/dto`

Key components include:
- RESTful API endpoints with versioning (`/api/v1`)
- OpenAPI/Swagger documentation
- Global exception handling
- Bean validation
- Flyway database migrations
- H2 in-memory database for development

## Data Flow and Business Logic

The system implements several core business flows:

### Customer 360 View
- Combines customer data with vehicle information, service records, and lead data
- Calculates churn risk based on vehicle mileage, warranty status, service history, and health status
- Integrates with churn prediction service to provide comprehensive customer insights

### Churn Prediction
- Analyzes vehicle data (mileage, warranty end date, health status)
- Evaluates service history to determine customer engagement
- Calculates risk score (0-100) with risk levels: LOW, MEDIUM, HIGH
- Provides detailed reasons for risk assessment

### Vehicle Assistant
- Manages vehicle interaction records
- Provides interaction history by vehicle ID

### Lead Management
- Handles lead creation and conversion
- Tracks lead status and source

### Stock Management
- Monitors inventory alerts
- Provides stock level information

## Key Endpoints

The API exposes endpoints for:
- Analytics: `/api/v1/analytics/overview`, `/api/v1/analytics/service-share`
- Vehicle Assistant: `/api/v1/vehicle-assistant/interactions`
- Churn prediction: `/api/v1/churn/customers/{customerId}`, `/api/v1/churn/risk-list`
- Customer 360: `/api/v1/customers/{customerId}/360`
- Leads: `/api/v1/leads`, `/api/v1/leads/{id}`
- Stock: `/api/v1/stock/alerts`

## Development Commands

To build and run the application:
```bash
mvn clean test
mvn spring-boot:run
```

To run a single test:
```bash
mvn test -Dtest=ClassName#methodName
```

To validate the API:
```bash
curl -s http://localhost:8080/v3/api-docs | head
curl -s http://localhost:8080/api/v1/churn/risk-list
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/vv3/api-docs`

## Database

- Uses H2 in-memory database for development
- Schema managed by Flyway migrations in `src/main/resources/db/migration/V1__create_schema.sql`
- Database console available at `http://localhost:8080/h2-console`

## Docker

The project includes Docker support for development:
- `Dockerfile` for building the production image
- `Dockerfile.claude` for development environment with Claude Code
- `docker-compose.dev.yml` for running in development mode