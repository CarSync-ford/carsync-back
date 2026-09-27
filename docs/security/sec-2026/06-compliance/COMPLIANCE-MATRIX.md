# Matriz de conformidade normativa — OWASP ASVS, API e Mobile

- **Data da avaliação:** 2026-09-26
- **Normas e edições avaliadas:**
  1. **OWASP Application Security Verification Standard (ASVS) v4.0.3**
  2. **OWASP API Security Top 10 (2023)**
  3. **OWASP Mobile Application Security Top 10 (2024)**
- **Aviso de escopo:** Esta matriz representa uma autoavaliação técnica de aderência baseada na implementação comprovada da solução CarSync. Não constitui certificação externa formal de auditoria independente.

---

## 1. OWASP API Security Top 10 (2023)

| Item | Título oficial | Status | Implementação no CarSync / Justificativa | Evidência técnica / Localização |
|---|---|---|---|---|
| **API1:2023** | Broken Object Level Authorization (BOLA) | **APLICADO** | Validação de posse de objeto nos serviços de negócio (ex: consultas de clientes e interações validam identificador e escopo do usuário autenticado). | `Customer360ServiceImpl.java`, `LeadServiceImpl.java`, testes de autorização por ID. |
| **API2:2023** | Broken Authentication | **APLICADO** | JJWT estrito com HS256 (chave >= 256 bits), TTL de acesso de 15 min, rotação de refresh token com revogação em banco, bloqueio temporário após 5 falhas consecutivas, suporte a MFA TOTP RFC 6238. | `JwtServiceImpl.java`, `AuthServiceImpl.java`, `SecurityConfig.java`, `AuthControllerIntegrationTest.java`. |
| **API3:2023** | Broken Object Property Level Authorization (BOPLA) | **APLICADO** | DTOs tipados com `@NotNull`, `@Size`, `@Pattern` para entrada e projeções estritas para saída (`Customer360Dto`, `LeadDto`). Impossibilidade de mass assignment de propriedades administrativas ou campos internos como IDs e senhas. | Pacote `dto/`, `GlobalExceptionHandler.java`. |
| **API4:2023** | Unrestricted Resource Consumption | **APLICADO** | Rate limiting ativo por IP via Bucket4j em `RateLimitFilter` (50 req/min geral, 5 req/min em endpoints sensíveis de autenticação), paginação em listagens e limites de tamanho em requisições HTTP. | `RateLimitFilter.java`, `RateLimitFilterTest.java`. |
| **API5:2023** | Broken Function Level Authorization (BFLA) | **PARCIAL** | Separação entre rotas públicas e autenticadas em `SecurityConfig`. Endpoints analíticos protegidos com `hasRole('ANALYST')`. Aguarda alinhamento com docente sobre perfis nominais (Brigadista, Gestor, Administrador). | `SecurityConfig.java`, `AnalyticsController.java`, testes em `SecurityConfigTest.java`. |
| **API6:2023** | Unrestricted Access to Sensitive Business Flows | **APLICADO** | Fluxos de autenticação, recuperação de senha (`/forgot-password` retorna 202 indistinto contra enumeração) e cálculo de churn protegidos por limites de invocação, assinatura HMAC e auditoria pós-commit. | `AuthServiceImpl.java`, `ChurnServiceImpl.java`, `HmacSignatureFilter.java`. |
| **API7:2023** | Server Side Request Forgery (SSRF) | **NÃO APLICÁVEL** | A API não consome nem busca URLs arbitrárias fornecidas pelo usuário em tempo de execução. Todas as comunicações externas são direcionadas para endpoints estáticos conhecidos. | Arquitetura de serviços; ausência de clientes HTTP arbitrários no código. |
| **API8:2023** | Security Misconfiguration | **APLICADO** | `SecurityConfig` restritivo com `SessionCreationPolicy.STATELESS`, desativação de CSRF desnecessário para APIs REST com JWT, cabeçalhos de segurança HTTP, contêiner non-root (`appuser:10001`), `read_only_rootfs: true`. | `SecurityConfig.java`, `Dockerfile`, `04-iot-infra/CONTAINER-HARDENING.md`. |
| **API9:2023** | Improper Inventory Management | **APLICADO** | Versionamento estrito na URI (`/api/v1`), documentação OpenAPI 3.1 com SpringDoc contendo esquema `bearerAuth` configurado e rotas descontinuadas eliminadas. | `OpenApiConfig.java`, `api-docs` validado em CI. |
| **API10:2023** | Unsafe Consumption of APIs | **PARCIAL** | Comunicação com broker IoT e serviços analíticos validada com TLS e DTOs tipados. Handoff de infraestrutura detalhado para broker externo. | `04-iot-infra/MQTT-TLS-HANDOFF.md`. |

---

## 2. OWASP ASVS v4.0.3 (Requisitos selecionados - Níveis L1 e L2)

| Capítulo ASVS | Controle | Requisito específico | Status | Evidência e implementação no projeto |
|---|---|---|---|---|
| **V1: Architecture** | V1.1.1 | Uso de modelagem de ameaças no ciclo de vida | **APLICADO** | Análise STRIDE completa documentada em `06-compliance/REPORT.md`. |
| **V2: Authentication** | V2.1.1 | Senhas armazenadas com função de derivação de chave com salt | **APLICADO** | BCrypt com fator de custo 10 implementado no `PasswordEncoder` do Spring Security. |
| **V2: Authentication** | V2.2.1 | Tokens de sessão/acesso com entropia mínima de 128 bits | **APLICADO** | JJWT com chave secreta HMAC-SHA256 >= 256 bits gerada criptograficamente. |
| **V2: Authentication** | V2.8.1 | Suporte a autenticação multifator (MFA) | **APLICADO** | MFA TOTP (RFC 6238) nativo em `/api/v1/auth/mfa/*` com geração de QR Code e verificação. |
| **V3: Session Management** | V3.2.1 | Rotação de identificadores de sessão após autenticação | **APLICADO** | Rotação atômica de refresh token a cada renovação (`/api/v1/auth/refresh`), revogando o token anterior. |
| **V3: Session Management** | V3.3.1 | Expiração e tempo limite de tokens inativos | **APLICADO** | Tokens de acesso com TTL estrito de 15 minutos; tokens de reset válidos por 15 minutos e uso único. |
| **V4: Access Control** | V4.1.1 | Aplicação do princípio do menor privilégio no controle de acesso | **PARCIAL** | Controle implementado via Spring Security com `ROLE_USER` e `ROLE_ANALYST`. Mapeamento de perfis de domínio aguarda validação. |
| **V5: Validation & Sanitization** | V5.1.1 | Validação server-side de todos os parâmetros recebidos | **APLICADO** | Jakarta Bean Validation em todos os DTOs de entrada (`@NotNull`, `@Size`, `@Email`, `@Pattern`). |
| **V5: Validation & Sanitization** | V5.3.1 | Prevenção contra injeção SQL/NoSQL | **APLICADO** | Uso estrito de Spring Data JPA com queries parametrizadas (Prepared Statements nativos do Hibernate). |
| **V7: Error Handling & Logging** | V7.1.1 | Não expor stacktraces ou dados sensíveis em erros | **APLICADO** | `GlobalExceptionHandler` captura todas as exceções e retorna payload estruturado uniforme `ApiErrorResponse`. |
| **V7: Error Handling & Logging** | V7.2.1 | Registro de auditoria estruturado de eventos de segurança | **APLICADO** | `JsonLogLayout` registrando `SECURITY_VIOLATION` e `SECURITY_AUDIT` em linha única JSON com mascaramento de PII. |
| **V8: Data Protection** | V8.1.1 | Proteção de dados em trânsito com TLS moderno | **APLICADO** | HTTPS forçado em produção (`api.carsync.me`), TLS 1.3 / 1.2 com cifras seguras no Ingress do Azure Container Apps. |
| **V8: Data Protection** | V8.3.1 | Proteção de dados confidenciais e PII em repouso | **APLICADO** | Mascaramento de dados com `DataMasker` e expurgo de dados inativos via `DataRetentionService`. |

---

## 3. OWASP Mobile Application Security Top 10 (2024)

*Avaliação realizada em articulação com a frente de frontend e o documento de handoff `03-frontends/FRONTEND-HANDOFF.md`.*

| Categoria | Título oficial | Status | Diretriz e mitigação arquitetural para o App CarSync | Dependência / Evidência |
|---|---|---|---|---|
| **M1:2024** | Improper Credential Usage | **APLICADO (Backend) / PENDENTE (App)** | Backend não aceita credenciais fixas; tokens temporários emitidos e rotacionados. O app deve armazenar tokens apenas em Keystore/Keychain. | `03-frontends/FRONTEND-HANDOFF.md`, testes de autenticação da API. |
| **M2:2024** | Inadequate Supply Chain Security | **APLICADO (Pipeline)** | Verificação contínua de dependências no pipeline com OWASP Dependency-Check e Semgrep. | `01-pipeline/REPORT.md`, workflows GitHub Actions. |
| **M3:2024** | Insecure Authentication/Authorization | **APLICADO** | O backend valida autenticação em todas as requisições sensíveis; nenhum controle é confiado exclusivamente à lógica client-side do aplicativo. | `SecurityConfig.java`, testes de endpoints protegidos. |
| **M4:2024** | Insufficient Input/Output Validation | **APLICADO** | Validação rigorosa no backend com Bean Validation e serialização segura com Jackson. | DTOs de entrada e `GlobalExceptionHandler`. |
| **M5:2024** | Insecure Communication | **APLICADO** | Comunicação móvel restrita a HTTPS (TLS 1.3/1.2). Recomenda-se implementação de SSL Pinning no cliente mobile conforme handoff. | Certificado válido da API (`api.carsync.me`), handoff em `03-frontends/`. |
| **M6:2024** | Inadequate Privacy Controls | **APLICADO** | API não retorna PII desnecessária em rotas públicas ou móveis; CPF e dados de contato mascarados em relatórios. | `DataMasker.java`, `06-compliance/LGPD.md`. |
| **M7:2024** | Insufficient Binary Protections | **PENDENTE (App)** | Aplicativo mobile deve ser compilado com ProGuard/R8 para ofuscação de bytecode e mitigação de engenharia reversa. | Tarefa delegada à equipe de desenvolvimento mobile. |
| **M8:2024** | Security Misconfiguration | **APLICADO** | Configurações seguras de CORS, headers HTTP e ausência de endpoints de debug expostos em produção. | `SecurityConfig.java`, `application-prod.yml`. |
| **M9:2024** | Insecure Data Storage | **PENDENTE (App)** | O aplicativo mobile não deve salvar credenciais em texto claro (SharedPreferences/UserDefaults); uso estrito de Android Keystore / iOS Keychain. | Detalhado com exemplos de código em `03-frontends/FRONTEND-HANDOFF.md`. |
| **M10:2024** | Insufficient Cryptography | **APLICADO** | Algoritmos fracos (MD5, SHA-1, DES) banidos do projeto. Uso exclusivo de BCrypt, HMAC-SHA256, AES-GCM e TLS 1.3. | `SecurityConfig.java`, `JwtServiceImpl.java`. |
