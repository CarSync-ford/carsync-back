# Graph Report - ArquiteturaOrientadaaServicos_Sprint1  (2026-09-23)

## Corpus Check
- 204 files · ~68,728 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 12 file(s) not represented in the graph (top: (none) 5, .ipynb 3, .example 2)

## Summary
- 1476 nodes · 3533 edges · 124 communities (71 shown, 53 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 432 edges (avg confidence: 0.81)
- Token cost: 121,000 input · 9,400 output

## Community Hubs (Navigation)
- User CRUD and Repositories
- CORS Config Validation Tests
- Bean Validation and CPF Validator
- HMAC and Rate Limit Filters
- MockMvc Integration Test Harness
- Auth DTO Contracts
- Churn Prediction Service
- User Entity Mapping
- Global Exception Handling
- PII Data Masking
- Security and OpenAPI Configuration
- Lead Entity Mapping
- Churn ML Classifier
- Vehicle Entity Mapping
- Analytics Controller Endpoints
- Auth Service Implementation
- DTO and Repository Interfaces
- Auth Service Unit Tests
- Expo App Manifest
- Churn Controller and DTOs
- Data Retention Scheduling
- JWT Filter and Service
- Assistant Interaction Entity
- REST Controller Mappings
- Mobile Dev Dependencies
- Retention Campaign API
- HTTPS and RBAC Security Tests
- Auth Controller with OpenAPI
- Dealership Entity and Smoke Tests
- Customer Entity Mapping
- Stock Item Entity Mapping
- VIN Risk Scoring API
- JWT Token Generation
- Customer Profile Screen
- JWT Secret Validation Tests
- JPA Entity and Envers Auditing
- Consultant Action Audit Log
- Mobile Alert and Risk Widgets
- Lead Conversion Flow
- Service Record Entity
- Mobile Runtime Dependencies
- Cached Request Body Wrapper
- Payload Size Limit Tests
- Defense-in-Depth Security Layers
- Stock Alert Prediction
- FastAPI App Bootstrap
- Python API Tests
- Security Feature Specs
- DevSecOps and Compliance Specs
- Python JWT Login
- Vehicle Assistant Service
- Expo Router Layouts
- Spring Context Fail-Fast Tests
- Login Flow Diagram
- JWT Key Initialization
- Application Bootstrap
- HMAC Signature Filter Tests
- LGPD and Auth Hardening Phases
- Mobile Dashboard Screens
- Dealership ROI and Service Share
- Password Reset Tokens
- Password Policy Tests
- Project Conventions and Architecture
- Feature Specs for Auth and Users
- Customer 360 Aggregation
- Analytics Service Implementation
- Core Schema Migration (Postgres)
- Core Schema Migration (H2)
- Radar Demo Screen
- Azure Deployment Pipeline
- JWT Filter Test Mocks
- Phase 0 Hardened Defaults
- Resource Not Found Handling
- Soft Delete Columns (Postgres)
- Soft Delete Columns (H2)
- Dashboard WebView Screen
- MFA TOTP Verification
- Envers Audit Tables (Postgres)
- Envers Audit Tables (H2)
- TypeScript Build Config
- Score Bar Component
- Mobile NPM Scripts
- Account Lockout Exception
- Vercel Deploy Config
- Duplicate Email Exception
- User Tables Migration (Postgres)
- Account Lockout Migration (Postgres)
- User Tables Migration (H2)
- Account Lockout Migration (H2)
- FordRetain Dashboard Variants
- Local LLM Proxy Setup
- Spec-Driven Flow Script
- Pull Request Summarizer Prompt
- Graphify Steering Rule
- Maven Project Descriptor
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- Isolated Migration Artifact
- FordRetain ML Requirements

## God Nodes (most connected - your core abstractions)
1. `User` - 48 edges
2. `Lead` - 40 edges
3. `Vehicle` - 39 edges
4. `Customer` - 34 edges
5. `UserRepository` - 33 edges
6. `AuthServiceTest` - 31 edges
7. `VehicleRepository` - 28 edges
8. `JwtService` - 26 edges
9. `CustomerRepository` - 25 edges
10. `AnalyticsSecurityIntegrationTest` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Project README` --semantically_similar_to--> `Deploy to Azure Container Apps Workflow`  [INFERRED] [semantically similar]
  README.md → .github/workflows/deploy.yml
- `P1 Harden Defaults Implementation Plan` --semantically_similar_to--> `Phase 0 - Quick Wins`  [INFERRED] [semantically similar]
  IMPLEMENTATION_PLAN_P1_HARDEN_DEFAULTS.md → .specs/phase-0-quick-wins.md
- `Project Conventions & Architecture` --semantically_similar_to--> `Layered Architecture (Controller/Service/Repository)`  [INFERRED] [semantically similar]
  CONVENTIONS.md → CLAUDE.md
- `HMAC-SHA256 request integrity/authenticity` --semantically_similar_to--> `LGPD data privacy (VIN SHA-256 hashing, anonymization)`  [INFERRED] [semantically similar]
  features/hmac_signature.md → test/ford-test-main/ford-test-main/FORDTESTE/fordretain/04_security/security_report.md
- `Rate Limiting with Bucket4j Spec` --semantically_similar_to--> `FordRetain Security Report`  [INFERRED] [semantically similar]
  features/rate_limiting.md → test/ford-test-main/ford-test-main/FORDTESTE/fordretain/04_security/security_report.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **DevSecOps Pipeline Security Stages** — specs_phase_3_devsecops_pipeline_semgrep_sast, specs_phase_3_devsecops_pipeline_owasp_dependency_check, specs_phase_3_devsecops_pipeline_gitleaks, specs_phase_3_devsecops_pipeline_trivy [EXTRACTED 1.00]
- **P1 Harden Defaults Bundle** — implementation_plan_p1_harden_defaults_hmac_default, implementation_plan_p1_harden_defaults_cors_failfast, implementation_plan_p1_harden_defaults_jwt_secret_validation, implementation_plan_p1_harden_defaults_security_headers [EXTRACTED 1.00]
- **Phased Security Roadmap (Phase 0-6)** — specs_phase_0_quick_wins_phase0, specs_phase_1_lgpd_critical_phase1, specs_phase_2_auth_hardening_phase2, specs_phase_3_devsecops_pipeline_phase3, specs_phase_4_observability_response_phase4, specs_phase_5_compliance_formal_phase5, specs_phase_6_scale_hardening_phase6 [EXTRACTED 1.00]
- **Security filter chain: HMAC then RateLimit then JWT auth** — features_hmac_signature, features_rate_limiting, features_get_user_jwt_auth_filter, features_actuator_and_security_refactor [INFERRED 0.85]
- **FordRetain analytical dashboards (api/mobile/combined)** — test_ford_test_main_ford_test_main_fordteste_fordretain_02_api_static_dashboard, test_ford_test_main_ford_test_main_fordteste_fordretain_03_mobile_fordretain_app_public_dashboard, test_ford_test_main_ford_test_main_fordteste_fordretain_dashboard [INFERRED 0.75]
- **User auth lifecycle: signup, login pre-auth JWT, get me** — features_post_user, features_auth_user, features_get_user [INFERRED 0.85]

## Communities (124 total, 53 thin omitted)

### Community 0 - "User CRUD and Repositories"
Cohesion: 0.06
Nodes (29): any, createuserrequest, getuserresponse, optional, org.junit.jupiter.api.BeforeEach, org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query (+21 more)

### Community 1 - "CORS Config Validation Tests"
Cohesion: 0.07
Nodes (6): org.junit.jupiter.api.Test, SecurityConfigCorsIntegrationTest, SecurityConfigCorsTest, SecurityConfigTest, UserControllerTest, JwtServiceImplIntegrationTest

### Community 2 - "Bean Validation and CPF Validator"
Cohesion: 0.08
Nodes (26): annotation, email, jakarta.validation.Constraint, jakarta.validation.ConstraintValidator, jakarta.validation.ConstraintValidatorContext, jakarta.validation.Payload, notblank, size (+18 more)

### Community 3 - "HMAC and Rate Limit Filters"
Cohesion: 0.09
Nodes (25): bandwidth, base64, concurrenthashmap, duration, io.github.bucket4j.Bucket, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletResponse, mac (+17 more)

### Community 4 - "MockMvc Integration Test Harness"
Cohesion: 0.12
Nodes (22): assertions, assertnotequals, autowired, com.fasterxml.jackson.databind.ObjectMapper, get, hassize, header, is (+14 more)

### Community 5 - "Auth DTO Contracts"
Cohesion: 0.10
Nodes (23): apiresponse, argumentmatchers, authenticationprincipal, authrequest, authresponse, bcrypt, changepasswordrequest, forgotpasswordrequest (+15 more)

### Community 6 - "Churn Prediction Service"
Cohesion: 0.18
Nodes (22): assertfalse, churnpredictionresponse, clock, collectors, function, instant, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry (+14 more)

### Community 7 - "User Entity Mapping"
Cohesion: 0.12
Nodes (4): PrePersist, Entity, Table, User

### Community 8 - "Global Exception Handling"
Cohesion: 0.24
Nodes (13): AccessDeniedException, jakarta.servlet.http.HttpServletRequest, jakarta.validation.ConstraintViolationException, org.springframework.http.ResponseEntity, org.springframework.security.access.AccessDeniedException, org.springframework.validation.FieldError, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice (+5 more)

### Community 9 - "PII Data Masking"
Cohesion: 0.13
Nodes (7): assertequals, assertnull, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.NullAndEmptySource, org.junit.jupiter.params.provider.ValueSource, DataMasker, DataMaskerTest

### Community 10 - "Security and OpenAPI Configuration"
Cohesion: 0.11
Nodes (22): contact, corsconfiguration, customizer, headersconfigurer, httpmethod, info, io.swagger.v3.oas.models.OpenAPI, license (+14 more)

### Community 12 - "Churn ML Classifier"
Cohesion: 0.10
Nodes (25): joblib, numpy, os, pandas, sklearn_cluster, sklearn_decomposition, sklearn_ensemble, sklearn_metrics (+17 more)

### Community 14 - "Analytics Controller Endpoints"
Cohesion: 0.20
Nodes (11): org.springframework.security.access.prepost.PreAuthorize, org.springframework.security.core.Authentication, AnalyticsController, AnalyticsDtos, AnalyticsOverviewResponse, CustomerAnalyticsView, LeadAnalyticsView, ServiceShareFilter (+3 more)

### Community 15 - "Auth Service Implementation"
Cohesion: 0.16
Nodes (4): org.springframework.transaction.annotation.Transactional, InvalidCredentialsException, AuthServiceImpl, Override

### Community 16 - "DTO and Repository Interfaces"
Cohesion: 0.13
Nodes (10): comparator, list, localdatetime, locale, notnull, org.springframework.data.jpa.repository.JpaRepository, LeadDtos, AssistantInteractionRepository (+2 more)

### Community 18 - "Expo App Manifest"
Cohesion: 0.08
Nodes (23): backgroundColor, adaptiveIcon, package, typedRoutes, expo, android, experiments, icon (+15 more)

### Community 19 - "Churn Controller and DTOs"
Cohesion: 0.16
Nodes (10): arraylist, chronounit, linkedhashset, ChurnController, ChurnDtos, ChurnPredictionResponse, VehicleChurnInsight, ChurnService (+2 more)

### Community 21 - "JWT Filter and Service"
Cohesion: 0.18
Nodes (6): org.springframework.test.context.TestPropertySource, Override, JwtAuthenticationFilter, JwtService, JwtAuthenticationFilterTest, JwtServiceTest

### Community 22 - "Assistant Interaction Entity"
Cohesion: 0.14
Nodes (3): AssistantInteraction, AssistantServiceImpl, Override

### Community 23 - "REST Controller Mappings"
Cohesion: 0.26
Nodes (13): mediatype, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.ResponseStatus, org.springframework.web.bind.annotation.RestController, pathvariable (+5 more)

### Community 24 - "Mobile Dev Dependencies"
Cohesion: 0.10
Nodes (19): @babel/core, expo, expo-constants, expo-linking, @expo/metro-runtime, expo-status-bar, react-dom, react-native-safe-area-context (+11 more)

### Community 25 - "Retention Campaign API"
Cohesion: 0.19
Nodes (16): BaseModel, Enum, pydantic, str, CampanhaRequest, CampanhaResultado, ClienteRiscoItem, ListaRisco (+8 more)

### Community 26 - "HTTPS and RBAC Security Tests"
Cohesion: 0.18
Nodes (3): org.junit.jupiter.api.DisplayName, HttpsSecurityTest, AnalyticsSecurityIntegrationTest

### Community 27 - "Auth Controller with OpenAPI"
Cohesion: 0.31
Nodes (9): io.swagger.v3.oas.annotations.Operation, io.swagger.v3.oas.annotations.responses.ApiResponses, io.swagger.v3.oas.annotations.security.SecurityRequirement, io.swagger.v3.oas.annotations.tags.Tag, org.springframework.security.core.userdetails.UserDetails, AuthController, PostMapping, RequestMapping (+1 more)

### Community 28 - "Dealership Entity and Smoke Tests"
Cohesion: 0.16
Nodes (3): org.springframework.boot.test.web.client.TestRestTemplate, Dealership, ArquiteturaOrientadaaServicosSprint1ApplicationTests

### Community 31 - "VIN Risk Scoring API"
Cohesion: 0.16
Nodes (12): fastapi, hashlib, re, ClienteScore, _h(), get, Gera inteiro determinístico 0..mod-1 a partir do VIN., Score e perfil determinísticos pelo hash do VIN — mesmo VIN sempre retorna o… (+4 more)

### Community 32 - "JWT Token Generation"
Cohesion: 0.21
Nodes (4): io.jsonwebtoken.Claims, JwtProperties, Override, JwtServiceImpl

### Community 33 - "Customer Profile Screen"
Cohesion: 0.18
Nodes (14): axios, COR_PERFIL, PerfilCliente(), SCRIPTS_PERFIL, styles, api, getScoreCliente(), mock() (+6 more)

### Community 34 - "JWT Secret Validation Tests"
Cohesion: 0.21
Nodes (7): assertdoesnotthrow, org.junit.jupiter.api.extension.ExtendWith, org.mockito.junit.jupiter.MockitoExtension, reflectiontestutils, JwtServiceImplValidationTest, standardcharsets, when

### Community 35 - "JPA Entity and Envers Auditing"
Cohesion: 0.35
Nodes (8): bigdecimal, column, generatedvalue, generationtype, jakarta.persistence.Entity, jakarta.persistence.Table, localdate, org.hibernate.envers.Audited

### Community 36 - "Consultant Action Audit Log"
Cohesion: 0.16
Nodes (13): datetime, json, logging, AcaoConsultor, AcaoRegistrada, historico_acoes(), get, post (+5 more)

### Community 37 - "Mobile Alert and Risk Widgets"
Cohesion: 0.14
Nodes (10): react-native, ALERTAS_MOCK, COR_TIPO, styles, ProfileHeaderProps, styles, corScore(), RiskCard() (+2 more)

### Community 38 - "Lead Conversion Flow"
Cohesion: 0.28
Nodes (5): LeadController, LeadConversionResponse, LeadResponse, ProactiveLeadRequest, LeadService

### Community 40 - "Mobile Runtime Dependencies"
Cohesion: 0.13
Nodes (15): dependencies, axios, expo, expo-constants, expo-linking, @expo/metro-runtime, expo-router, expo-status-bar (+7 more)

### Community 41 - "Cached Request Body Wrapper"
Cohesion: 0.19
Nodes (9): bufferedreader, bytearrayinputstream, inputstreamreader, ioexception, jakarta.servlet.http.HttpServletRequestWrapper, jakarta.servlet.ServletInputStream, readlistener, CachedBodyHttpServletRequest (+1 more)

### Community 42 - "Payload Size Limit Tests"
Cohesion: 0.18
Nodes (11): bytearrayresource, http, linkedmultivaluemap, multivaluemap, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Import, org.springframework.web.multipart.MultipartFile, PayloadLimitTest.TestUploadController (+3 more)

### Community 43 - "Defense-in-Depth Security Layers"
Cohesion: 0.16
Nodes (14): Backend Security & Architecture Changes, Cloudflare Edge (TLS/WAF/DDoS), Hibernate Envers Audit Trail, HmacSignatureFilter, JWT Authentication (JwtServiceImpl), RateLimitFilter (Bucket4j), RBAC (@EnableMethodSecurity), SECURITY_VIOLATION Anomaly Logs (+6 more)

### Community 44 - "Stock Alert Prediction"
Cohesion: 0.22
Nodes (6): StockController, StockAlertItem, StockDtos, StockPredictionResponse, Override, StockService

### Community 45 - "FastAPI App Bootstrap"
Cohesion: 0.19
Nodes (10): fastapi_middleware_cors, fastapi_responses, fastapi_staticfiles, slowapi, slowapi_errors, slowapi_util, dashboard(), health() (+2 more)

### Community 46 - "Python API Tests"
Cohesion: 0.21
Nodes (7): fastapi_testclient, sys, get_token(), test_clientes_risco(), test_score_cliente(), test_score_vin_invalido(), test_service_share()

### Community 47 - "Security Feature Specs"
Cohesion: 0.18
Nodes (13): Actuator + SecurityConfig Refactor Spec, Actuator liveness/readiness probes, HMAC Payload Signature Filter Spec, HMAC-SHA256 request integrity/authenticity, Payload Limit (1MB multipart) Spec, Rate Limiting with Bucket4j Spec, Explicit Spring Security endpoint allowlist, LGPD data privacy (VIN SHA-256 hashing, anonymization) (+5 more)

### Community 48 - "DevSecOps and Compliance Specs"
Cohesion: 0.19
Nodes (13): Ford Challenge DevSecOps Requirements, Security Checklist & Implementation Plan, Gitleaks Secret Scanning, OWASP Dependency Check SCA, Phase 3 - DevSecOps Pipeline, Semgrep SAST, Trivy Container Security, Incident Response Plan (NIST SP 800-61) (+5 more)

### Community 49 - "Python JWT Login"
Cohesion: 0.24
Nodes (10): fastapi_security, jose, passlib_context, criar_token(), verificar_token(), LoginRequest, TokenResponse, login() (+2 more)

### Community 50 - "Vehicle Assistant Service"
Cohesion: 0.32
Nodes (5): AssistantController, AssistantDtos, VehicleAssistantRequest, VehicleAssistantResponse, AssistantService

### Community 51 - "Expo Router Layouts"
Cohesion: 0.22
Nodes (6): expo-router, react, @react-native-async-storage/async-storage, LoginScreen(), styles, login()

### Community 52 - "Spring Context Fail-Fast Tests"
Cohesion: 0.31
Nodes (8): assertnotnull, assertthrows, asserttrue, beancreationexception, configurableapplicationcontext, nestedexceptionutils, springapplicationbuilder, webapplicationtype

### Community 53 - "Login Flow Diagram"
Cohesion: 0.22
Nodes (10): Auth Login Flow Diagram, Return 400 with Fake Validation, Username & Password Format Validation, Password Correctness Check, POST auth/login Request, Return 400 Bad Request, Return Auth Token and Refresh Token, Salt and Compare Sent Password (+2 more)

### Community 54 - "JWT Key Initialization"
Cohesion: 0.20
Nodes (6): date, jakarta.annotation.PostConstruct, jwts, keys, secretkey, uuid

### Community 55 - "Application Bootstrap"
Cohesion: 0.29
Nodes (7): loggerfactory, org.slf4j.Logger, org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.boot.CommandLineRunner, org.springframework.boot.context.properties.EnableConfigurationProperties, springapplication, ArquiteturaOrientadaaServicosSprint1Application

### Community 57 - "LGPD and Auth Hardening Phases"
Cohesion: 0.20
Nodes (10): ANALYST Role, Anonymization Pipeline (SEC-001), Data Retention / Secure Disposal (SEC-002), DataMasker Utility, Phase 1 - LGPD Critical, Account Lockout / Brute-force Protection, MFA TOTP (RFC 6238), Password Reset Flow (+2 more)

### Community 58 - "Mobile Dashboard Screens"
Cohesion: 0.31
Nodes (8): ClientesScreen(), corScore(), styles, corScore(), PainelPrincipal(), styles, getClientesEmRisco(), getRoiConcessionaria()

### Community 59 - "Dealership ROI and Service Share"
Cohesion: 0.31
Nodes (8): random, ROISummary, ServiceShareConcessionaria, get, Índice de Service Share atual da concessionária com tendência e clientes em…, Calcula o ROI estimado de retenção para a concessionária com base nos VINs em…, roi_concessionaria(), service_share()

### Community 62 - "Project Conventions and Architecture"
Cohesion: 0.32
Nodes (8): Aider Configuration, Conventional Commits Standard, Churn Prediction Flow, CLAUDE.md Project Guidance, Customer 360 View Flow, Layered Architecture (Controller/Service/Repository), Project Conventions & Architecture, Project README

### Community 63 - "Feature Specs for Auth and Users"
Cohesion: 0.29
Nodes (8): POST /api/v1/auth Login with pre-auth JWT Spec, Anti-timing dummy bcrypt countermeasure, Pre-auth JWT (scope=pre-auth) for MFA step, GET /api/v1/user/me Spec, JwtAuthenticationFilter concept, POST /api/v1/user Create User Spec, CPF check-digit validation and uniqueness, Feature Spec Template

### Community 64 - "Customer 360 Aggregation"
Cohesion: 0.32
Nodes (3): Customer360Dtos, Customer360Response, CustomerVehicleResponse

### Community 66 - "Core Schema Migration (Postgres)"
Cohesion: 0.61
Nodes (7): assistant_interactions, customers, dealerships, leads, service_records, stock_items, vehicles

### Community 67 - "Core Schema Migration (H2)"
Cohesion: 0.61
Nodes (7): assistant_interactions, customers, dealerships, leads, service_records, stock_items, vehicles

### Community 68 - "Radar Demo Screen"
Cohesion: 0.25
Nodes (6): END, H_ROADS, s, START, V_ROADS, { width: SCREEN_W }

### Community 69 - "Azure Deployment Pipeline"
Cohesion: 0.33
Nodes (7): Azure Security & Infrastructure Doc, Encryption at Rest (Microsoft-managed keys), Log Analytics Workspace (ContainerAppConsoleLogs_CL), ACA Secrets Management, Docker Build and Deploy Job, Deploy to Azure Container Apps Workflow, Maven Test Job

### Community 70 - "JWT Filter Test Mocks"
Cohesion: 0.29
Nodes (6): claims, defaultclaims, mock, mockhttpservletrequest, mockhttpservletresponse, mockito

### Community 71 - "Phase 0 Hardened Defaults"
Cohesion: 0.52
Nodes (7): SEC-005 CORS Fail-Fast Validation, SEC-003 HMAC Enabled by Default, SEC-010 JWT Secret Min Length Validation, P1 Harden Defaults Implementation Plan, SEC-011 OWASP Security Headers, Security Review Report (OWASP-based), Phase 0 - Quick Wins

### Community 73 - "Soft Delete Columns (Postgres)"
Cohesion: 0.29
Nodes (6): idx_customers_deleted_at, idx_leads_deleted_at, idx_users_deleted_at, customers, leads, users

### Community 74 - "Soft Delete Columns (H2)"
Cohesion: 0.29
Nodes (6): idx_customers_deleted_at, idx_leads_deleted_at, idx_users_deleted_at, customers, leads, users

### Community 75 - "Dashboard WebView Screen"
Cohesion: 0.38
Nodes (5): DashboardScreen(), PERFIS_MOCK, styles, DASHBOARD_HTML, getServiceShare()

### Community 77 - "Envers Audit Tables (Postgres)"
Cohesion: 0.60
Nodes (5): customers_aud, leads_aud, revinfo, user_type_aud, users_aud

### Community 78 - "Envers Audit Tables (H2)"
Cohesion: 0.60
Nodes (5): customers_aud, leads_aud, revinfo, user_type_aud, users_aud

### Community 79 - "TypeScript Build Config"
Cohesion: 0.40
Nodes (4): expo/tsconfig.base, compilerOptions, extends, include

### Community 80 - "Score Bar Component"
Cohesion: 0.50
Nodes (4): corScore(), ScoreBar(), ScoreBarProps, styles

### Community 81 - "Mobile NPM Scripts"
Cohesion: 0.40
Nodes (5): scripts, android, ios, start, web

### Community 83 - "Vercel Deploy Config"
Cohesion: 0.50
Nodes (3): buildCommand, outputDirectory, rewrites

### Community 89 - "FordRetain Dashboard Variants"
Cohesion: 0.67
Nodes (3): FordRetain API static dashboard.html, FordRetain mobile app dashboard.html, FordRetain combined dashboard.html

## Knowledge Gaps
- **125 isolated node(s):** `br.com.sprint1.challenge:ArquiteturaOrientadaaServicos_Sprint1`, `sdd-flow.sh script`, `ServiceShareFilter`, `name`, `slug` (+120 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 430 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **53 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Vehicle` connect `Vehicle Entity Mapping` to `JPA Entity and Envers Auditing`, `MockMvc Integration Test Harness`, `Churn Prediction Service`, `DTO and Repository Interfaces`, `Churn Controller and DTOs`, `Data Retention Scheduling`, `Dealership Entity and Smoke Tests`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Why does `JwtService` connect `JWT Filter and Service` to `User CRUD and Repositories`, `JWT Token Generation`, `CORS Config Validation Tests`, `HMAC and Rate Limit Filters`, `MockMvc Integration Test Harness`, `Auth DTO Contracts`, `JWT Filter Test Mocks`, `Payload Size Limit Tests`, `Dealership Entity and Smoke Tests`, `Auth Service Implementation`, `Auth Service Unit Tests`, `JWT Key Initialization`, `HTTPS and RBAC Security Tests`, `Password Reset Tokens`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `Customer` connect `Customer Entity Mapping` to `JPA Entity and Envers Auditing`, `MockMvc Integration Test Harness`, `Churn Prediction Service`, `User Entity Mapping`, `Vehicle Entity Mapping`, `DTO and Repository Interfaces`, `Churn Controller and DTOs`, `Data Retention Scheduling`, `Dealership Entity and Smoke Tests`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **What connects `br.com.sprint1.challenge:ArquiteturaOrientadaaServicos_Sprint1`, `sdd-flow.sh script`, `ServiceShareFilter` to the rest of the system?**
  _125 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `User CRUD and Repositories` be split into smaller, more focused modules?**
  _Cohesion score 0.05674044265593561 - nodes in this community are weakly interconnected._
- **Should `CORS Config Validation Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.07342995169082125 - nodes in this community are weakly interconnected._
- **Should `Bean Validation and CPF Validator` be split into smaller, more focused modules?**
  _Cohesion score 0.07862679955703211 - nodes in this community are weakly interconnected._