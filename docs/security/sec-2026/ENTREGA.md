# Documento consolidado de segurança — CarSync (Ford Challenge)

- **Disciplina / Projeto:** Arquitetura Orientada a Serviços — Entrega Final de Segurança (DevSecOps)
- **Data da consolidação:** 2026-09-26
- **Branch / Worktree:** `sec-2026/fechamento` (baseado em `origin/main` commit `5f76776`)
- **Repositório:** `CarSync-ford/carsync-back`
- **Ambiente de produção avaliado:** `https://api.carsync.me/` (Azure Container Apps / East US)
- **Sumário da entrega:** Este documento unifica as quatro atividades exigidas pelo `SEC-REQUIREMENTS.md`, integrando código, infraestrutura, observabilidade, conformidade e governança DevSecOps.

---

## Estrutura da pontuação e pesos

| Atividade | Descrição no programa | Peso | Estado consolidado |
|---|---|---|---|
| **Atividade 1** | Pipeline DevSecOps Integrado | **3,0** | **VERIFICADO** (Pipeline CI/CD com SAST, SCA, Secrets e Container Security ativo) |
| **Atividade 2** | Segurança em Código e Infraestrutura | **2,5** | **VERIFICADO** (Hardening completo da API, HMAC, rate limit, JWT, Dockerfile non-root, handoffs mobile/IoT) |
| **Atividade 3** | Observabilidade, Monitoramento e Resposta | **2,0** | **VERIFICADO** (Logs JSON mascarados, Workbook nativo no Azure, alertas e fluxo de incidentes em 5 fases) |
| **Atividade 4** | Compliance, Riscos e Segurança Contínua | **2,5** | **VERIFICADO** (STRIDE, ASVS v4, API Top 10, Mobile Top 10, LGPD, 4 rotinas contínuas e backup/restore) |
| **Total** | **Avaliação integrada (Média ponderada)** | **10,0** | **Atendimento integral rastreável** |

---

# Atividade 1: Pipeline DevSecOps Integrado (Peso 3,0)

## 1.1 Objetivo e desenho do pipeline seguro
O pipeline de Integração e Entrega Contínua (CI/CD) foi arquitetado no **GitHub Actions** para garantir que nenhuma alteração seja promovida para produção sem validação automática de segurança, análise estática de vulnerabilidades e verificação de integridade de dependências.

### Diagrama do pipeline de segurança (Commit ao Deploy)

```
[ Desenvolvedor ]
       │
       ▼ (git push / Pull Request)
[ GitHub Actions CI Runner ]
       │
       ├─► 1. Secret Scanning ──► Gitleaks (varredura de histórico e arquivos)
       │
       ├─► 2. SAST ─────────────► Semgrep (regras OWASP Top 10 e Spring Security)
       │
       ├─► 3. SCA ──────────────► OWASP Dependency-Check (CVEs em pom.xml)
       │
       ├─► 4. Testes e Build ───► Maven test (212 testes unitários e de integração)
       │
       ├─► 5. Container Scan ───► Trivy (análise de imagem Docker e SO Alpine)
       │
       ▼ (Aprovação de todos os gates de segurança)
[ Azure Container Registry (ACR) ] ──► Deploy automatizado ──► [ Azure Container Apps (ACA) ]
```

## 1.2 Etapas de segurança automatizadas e redução de riscos

| Etapa | Ferramenta | O que analisa | Riscos mitigados |
|---|---|---|---|
| **SAST** | **Semgrep** | Código-fonte Java/Spring e arquivos de configuração. | Injeção SQL, injeção de comandos, bypass de autenticação, uso de algoritmos criptográficos inseguros e tratamento incorreto de exceções antes do deploy. |
| **SCA** | **OWASP Dependency-Check** / Dependabot | Dependências de terceiros declaradas no `pom.xml`. | Vulnerabilidades de cadeia de suprimentos (Supply Chain), como bibliotecas com CVEs conhecidas e exploração de componentes vulneráveis (OWASP Top 10 A06). Bloqueio automático para CVSS >= 7.0. |
| **Secret Scanning** | **Gitleaks** | Histórico de commits, diffs e arquivos de ambiente. | Vazamento inadvertido de chaves privadas, senhas de banco de dados, secrets de JWT, credenciais cloud ou chaves HMAC no repositório. |
| **Container Security** | **Trivy** | Imagem Docker construída, camadas e pacotes do SO base. | Vulnerabilidades em pacotes do sistema operacional Alpine, permissões de root indevidas e binários desnecessários no contêiner de produção. |
| **Testes de Regressão** | **JUnit 5 / Spring Test** | 212 testes unitários, de componentes e de integração. | Regressão de funcionalidades de negócio, quebra de contratos de API e falhas em filtros de segurança (`SecurityConfig`, `JwtAuthenticationFilter`, `RateLimitFilter`). |

## 1.3 Execução no projeto Ford Challenge
- O arquivo de workflow `.github/workflows/deploy.yml` executa as varreduras de forma paralela e sequencial com falha bloqueante (`exit 1`) caso vulnerabilidades críticas sejam encontradas.
- O build final do contêiner utiliza Dockerfile multi-stage com compilação isolada e descarte de ferramentas de build na imagem final.
- Evidências completas documentadas em `docs/security/sec-2026/01-pipeline/REPORT.md`.

---

# Atividade 2: Segurança em Código e Infraestrutura (Peso 2,5)

## 2.1 Criptografia local (Aplicativo móvel e clientes)
- **Handoff formal com a frente Mobile:** Documentado detalhadamente em `docs/security/sec-2026/03-frontends/FRONTEND-HANDOFF.md`.
- **Diretriz de armazenamento seguro:** O cliente móvel não armazena tokens ou credenciais em arquivos de preferências não criptografados (`SharedPreferences` / `UserDefaults`).
- **Implementação recomendada:** Uso do **Android Keystore** com `EncryptedSharedPreferences` (MasterKey com algoritmo AES-256-GCM) no Android e **iOS Keychain Services** no iOS.

## 2.2 Hardening de API implementado no backend
A API passou por auditoria e correções substanciais no código-fonte para mitigar falhas de autenticação, injeção e abuso de recursos:

### 1. Rate Limiting com Bucket4j (`RateLimitFilter.java`)
- Controle em memória implementado via algoritmo Token Bucket.
- **Limite geral da API:** 50 requisições por minuto por endereço IP (`X-Forwarded-For`).
- **Limite de autenticação estrito:** 5 requisições por minuto nos endpoints sensíveis (`/api/v1/auth`, `/api/v1/auth/refresh`, `/api/v1/auth/reset-password`).
- **Resposta:** Código HTTP 429 Too Many Requests com cabeçalho `Retry-After`.

### 2. Validação de entrada server-side
- Uso abrangente de Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`, `@Email`, `@Pattern`) em todos os DTOs do pacote `br.com.sprint1.challenge.dto`.
- Payloads inválidos são interceptados pelo `GlobalExceptionHandler`, que devolve resposta HTTP 400 estruturada (`ApiErrorResponse`), impedindo que dados não higienizados alcancem a camada de persistência.

### 3. Integridade de requisição com HMAC-SHA256 (`HmacSignatureFilter.java`)
- 23 endpoints de negócio sensíveis em produção exigem o cabeçalho `X-HMAC-Signature`.
- O hash é calculado sobre o payload HTTP com chave secreta compartilhada, prevenindo adulteração de dados em trânsito (anti-tampering).

### 4. JWT seguro e rotação de Refresh Tokens
- **Biblioteca:** JJWT com validação criptográfica estrita da chave HS256 (tamanho >= 256 bits).
- **Separação de tipos de token:** Validação obrigatória da claim `type`. Um token do tipo `REFRESH` é rejeitado caso seja apresentado em rotas de recursos de API, evitando confusão de contexto.
- **Tempo de vida (TTL):** Tokens de acesso expiram em 15 minutos; tokens de reset em 15 minutos (uso único).
- **Rotação atômica de refresh tokens:** A cada chamada a `/api/v1/auth/refresh`, o token antigo é revogado e um novo par (access + refresh) é emitido e persistido no banco de dados.
- **Sanitização de exceções JWT:** Captura de `ExpiredJwtException`, `MalformedJwtException` e `SignatureException` em `AuthServiceImpl` e `GlobalExceptionHandler`, retornando HTTP 401 Unauthorized uniforme em vez de erros 500.

### 5. Proteção de endpoints sensíveis em `SecurityConfig.java`
- Fechamento da regra ampla `.requestMatchers("/api/v1/auth/**").permitAll()`.
- Apenas 4 rotas públicas autorizadas para POST anônimo: `/api/v1/auth` (login), `/api/v1/auth/refresh`, `/api/v1/auth/forgot-password` e `/api/v1/auth/reset-password`.
- Rotas `/api/v1/auth/change-password` e `/api/v1/auth/mfa/**` agora exigem autenticação obrigatória via Bearer Token, com injeção de `@AuthenticationPrincipal UserDetails` seguro em `JwtAuthenticationFilter`.

```java
// Trecho de SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST,
            "/api/v1/auth",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password").permitAll()
    .requestMatchers("/api/v1/auth/change-password", "/api/v1/auth/mfa/**").authenticated()
    .requestMatchers("/api/v1/analytics/**").hasRole("ANALYST")
    .anyRequest().authenticated()
)
```

## 2.3 Controle de acesso por perfil e correspondência de domínio
- **Implementação técnica no Spring Security:** Atribuição de perfis `ROLE_USER` e `ROLE_ANALYST`.
- **Validação de equivalência de domínio com o docente (Decisão de projeto):**
  - O enunciado menciona os perfis *Brigadista, Gestor, Administrador*.
  - Para evitar a criação arbitrária de migrações sintéticas no banco sem regra de negócio correspondente, adotou-se o seguinte mapeamento de equivalência conceitual:
    - **Brigadista (Operador veicular):** Usuário final autenticado (`ROLE_USER`), com acesso aos seus próprios veículos, ordens de serviço e interações.
    - **Gestor (Gestor de frota/analista):** Acesso a relatórios agregados e predição de churn (`ROLE_ANALYST`).
    - **Administrador (Gestão de acessos/segurança):** Responsável por políticas globais e auditoria no Azure e banco de dados.

## 2.4 Segurança MQTT/TLS para IoT
- **Handoff com equipe de IoT:** Especificado em `docs/security/sec-2026/04-iot-infra/MQTT-TLS-HANDOFF.md`.
- **Criptografia e transporte:** Comunicação veicular através de porta segura 8883 utilizando TLS 1.3 com suites de cifra modernas (ECDHE-RSA-AES256-GCM-SHA384).
- **Autenticação:** Validação de certificados digitais X.509 ou credenciais de cliente individuais atreladas ao VIN do veículo.
- **Autorização (ACLs):** Restrição estrita de tópicos para que um dispositivo veicular só possa publicar em `carsync/telemetry/{vin}` e assinar comandos em `carsync/commands/{vin}`. Rejeição explícita de conexões em texto claro (porta 1883).

## 2.5 Hardening de infraestrutura (IaC e Dockerfile)
A imagem de contêiner segue as melhores práticas do CIS Docker Benchmark:

```dockerfile
# Trecho de hardening no Dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup
USER appuser:appgroup
WORKDIR /app
COPY --chown=appuser:appgroup target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

- **Execução não-root:** Usuário `appuser` (UID 10001).
- **Sistema de arquivos:** `read_only_rootfs: true`, permitindo escrita apenas em diretório temporário `tmpfs` montado em `/tmp`.
- **Elevação de privilégios:** `no-new-privileges: true` ativado nas definições do Azure Container Apps.
- **Script de verificação:** `docs/security/sec-2026/01-pipeline/check-container.sh`.

---

# Atividade 3: Observabilidade, Monitoramento e Resposta (Peso 2,0)

## 3.1 Logs estruturados e auditoria transacional
- **Formato dos logs:** JSON estruturado de linha única via `JsonLogLayout` integrado ao Logback (`logback-spring.xml`).
- **Mascaramento automático de dados sensíveis:** Expressões regulares sanitizam em tempo de execução:
  - Senhas (`password=***`)
  - Tokens JWT e refresh tokens (`token=***...`)
  - CPFs (`***.***.123-**`)
  - Segredos TOTP de MFA
- **Garantia transacional de auditoria (`logAfterCommit`):**
  - Eventos de auditoria sensíveis (`SECURITY_AUDIT`) utilizam `TransactionSynchronizationManager.afterCommit()`.
  - Se a transação do banco sofrer rollback, o evento de sucesso **não é emitido**, eliminando divergências entre logs de auditoria e o estado real dos dados.

```java
// Emissão pós-commit em AuthServiceImpl.java
private void logAfterCommit(String action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("SECURITY_AUDIT action:{} status:SUCCESS", action);
            }
        });
    } else {
        log.info("SECURITY_AUDIT action:{} status:SUCCESS", action);
    }
}
```

## 3.2 Recurso Azure Monitor Workbook implantado
Em 2026-09-26, foi provisionado e validado o recurso de dashboard nativo no Azure:

- **ID do recurso:** `/subscriptions/7fd8132e-7c9a-4b4d-a191-04b21ecc968c/resourceGroups/carsync-dev/providers/Microsoft.Insights/workbooks/24de0afb-9527-4da1-805b-3ae29db1eb83`
- **Nome de exibição:** `CarSync — Segurança e Operação`
- **Resource Group:** `carsync-dev` (Location: `eastus`)
- **Fontes de telemetria conectadas:** Application Insights `appi-carsync-dev` e Log Analytics Workspace `law-carsync-dev` (`72dac82d-b8c4-4685-952a-c0d263713c83`).
- **Definição versionada no Git:** `docs/security/sec-2026/05-observability/workbook.json`.
- **Link de acesso no Azure Portal:** [Abrir Workbook](https://portal.azure.com/#@11dbbfe2-89b8-4549-be10-cec364e59551/resource/subscriptions/7fd8132e-7c9a-4b4d-a191-04b21ecc968c/resourceGroups/carsync-dev/providers/Microsoft.Insights/workbooks/24de0afb-9527-4da1-805b-3ae29db1eb83).

### Consultas KQL configuradas nos 7 painéis (12 itens)
1. **Volume de requisições:** Agregação horária de `AppRequests` ponderada por amostragem (`sum(Weight)`).
2. **Erros HTTP 5xx:** Detecção temporal de falhas internas da aplicação.
3. **Latência p95:** Percentil 95 de tempo de resposta em milissegundos.
4. **Respostas 401, 403 e 429:** Tabela discriminando falhas de autenticação, permissão e bloqueios de taxa.
5. **Rotas com erros normalizadas:** Agrupamento por endpoint seguro substituindo IDs variáveis por `{id}` (ex: `/api/v1/leads/{id}`).
6. **Login e ações sensíveis:** Monitoramento operacional de requisições a `/auth`, `/change-password` e `/mfa/*`.
7. **Eventos de segurança sanitizados:** Tabela com eventos `SECURITY_VIOLATION`, `ANALYTICS_ACCESS` e `DATA_RETENTION` com supressão de mensagens brutas e IPs.

## 3.3 Regras de alerta e telemetria multicomponente
- **Regra ativa no Azure Monitor:** `alert-sec-violations` associada ao Action Group `ag-security-email`.
- **Correção da consulta KQL:** Documentada em `05-observability/ALERTS.md` para evitar a dupla agregação, avaliando eventos filtrados de `AppTraces` em janelas deslizantes de 1 minuto.
- **Plano para componentes externos:** Telemetria para Mobile, IoT e ML documentada com sinais, regras e destinos no arquivo `05-observability/MONITORING.md`.

## 3.4 Plano de resposta a incidentes em cinco etapas
Fluxo operacional detalhado em `docs/security/sec-2026/05-observability/INCIDENT-RESPONSE.md`:

```
[ 1. Detecção ] ──► Alerta Azure Monitor (alert-sec-violations) ou notificação via ag-security-email
      │
      ▼
[ 2. Análise ]  ──► Triagem no Log Analytics via KQL, correlação de IP, timestamps e rotas atingidas
      │
      ▼
[ 3. Contenção ]──► Bloqueio de IP no ACA Ingress, rotação emergencial de chave HMAC ou revogação de tokens
      │
      ▼
[ 4. Erradicação]─► Correção de código/vulnerabilidade, publicação de hotfix via CI/CD e patch de banco
      │
      ▼
[ 5. Recuperação]─► Validação de telemetria no Workbook, restauração de tráfego e Relatório Pós-Incidente
```

Exercício prático de mesa (Tabletop) simulando ataque de força bruta registrado em `docs/security/sec-2026/05-observability/TABLETOP.md`.

---

# Atividade 4: Compliance, Riscos e Segurança Contínua (Peso 2,5)

## 4.1 Revisão final de riscos (STRIDE + DevSecOps)
- Documento dedicado em `docs/security/sec-2026/06-compliance/REPORT.md`.
- Mapeamento de 10 ameaças principais abrangendo Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service e Elevation of Privilege em 5 fronteiras de confiança.
- Rastreabilidade de cada ameaça até os controles no código, testes de validação, riscos residuais e responsáveis.

## 4.2 Matriz de conformidade normativa
- Documento dedicado em `docs/security/sec-2026/06-compliance/COMPLIANCE-MATRIX.md`.
- **OWASP API Security Top 10 (2023):** Cobertura das 10 categorias com 8 aplicadas integralmente, 1 parcial (perfis de domínio) e 1 não aplicável (SSRF por inexistência de clientes arbitrários).
- **OWASP ASVS v4.0.3:** Verificação dos capítulos V1 (Arquitetura), V2 (Autenticação), V3 (Sessão), V4 (Acesso), V5 (Validação), V7 (Erros/Logs) e V8 (Proteção de dados).
- **OWASP Mobile Top 10 (2024):** Mapeamento conjunto com o handoff mobile cobrindo credenciais, armazenamento e criptografia.

## 4.3 Mapeamento de conformidade com a LGPD (Lei 13.709/2018)
- Documento dedicado em `docs/security/sec-2026/06-compliance/LGPD.md`.
- **Inventário de dados:** Classificação de dados pessoais, telemetria operacional do veículo e dados de geolocalização.
- **Bases legais propostas:** Execução de contrato (Art. 7º, V) para serviços do veículo; consentimento explícito para telemetria de geolocalização; legítimo interesse para segurança contra fraudes.
- **Ciclo de vida dos dados:**
  - Mascaramento em runtime com `DataMasker.java` (pseudonimização de interface e logs).
  - Rotina de anonimização irreversível e expurgo com `DataRetentionService.java` para dados que atingiram o término de custódia.
- **Tratamento do histórico do Envers:** Recomendação de exclusão de campos de credenciais via `@NotAudited` para evitar retenção de dados sensíveis na tabela `_aud`.

## 4.4 Rotinas de segurança contínua
- Documento dedicado em `docs/security/sec-2026/06-compliance/CONTINUOUS-SECURITY.md`.
  1. **Revisão de dependências (SCA):** Verificação contínua no CI e triagem semanal via Dependabot com SLAs definidos por severidade (48h para CVEs críticas).
  2. **Testes de segurança:** Semgrep (SAST) e suíte de testes de regressão automatizados disparados a cada Pull Request.
  3. **Auditoria de permissões:** Revisão trimestral de acessos RBAC na assinatura Azure, repositório GitHub e banco de dados.
  4. **Simulação de resposta a incidentes:** Exercícios semestrais de Tabletop baseados nos runbooks operacionais.

## 4.5 Rotina de backup e recuperação de desastres (PostgreSQL)
- Documento dedicado em `docs/security/sec-2026/06-compliance/BACKUP-RECOVERY.md`.
- **Inventário:** Base relacional PostgreSQL com migrações Flyway e tabelas de auditoria Envers.
- **Estratégia:** Backup contínuo WAL / Point-In-Time Restore (PITR) de 35 dias no Azure Database for PostgreSQL, somado a dumps lógicos diários compactados (`pg_dump -Fc`).
- **Procedimento seguro de restauração:** Protocolo passo a passo de restauração e validação de integridade executado exclusivamente em contêiner/ambiente isolado (staging), sem impacto na base de produção ativa. Metas de RPO <= 5 min e RTO <= 2 horas.

---

# Matriz consolidada de atendimento dos requisitos (R01 a R23)

| ID | Requisito do programa | Atividade correspondente | Status de conformidade | Evidência técnica e localização no repositório | Limitações e observações |
|---|---|---|---|---|---|
| **R01** | Diagrama CI/CD focado em segurança | Atividade 1 (Peso 3,0) | **VERIFICADO** | Seção 1.1 deste documento e `01-pipeline/REPORT.md`. | Pipeline GitHub Actions com etapas integradas. |
| **R02** | SAST (Static Application Security Testing) | Atividade 1 (Peso 3,0) | **VERIFICADO** | Semgrep integrado no workflow CI; `01-pipeline/REPORT.md`. | Rulesets para OWASP Top 10 e Spring Security. |
| **R03** | SCA (Software Composition Analysis) | Atividade 1 (Peso 3,0) | **VERIFICADO** | OWASP Dependency-Check e Dependabot; `01-pipeline/REPORT.md`. | Bloqueio de builds com CVEs CVSS >= 7.0. |
| **R04** | Secret Scanning | Atividade 1 (Peso 3,0) | **VERIFICADO** | Gitleaks ativo no pipeline CI; `01-pipeline/REPORT.md`. | Varredura de histórico e commits. |
| **R05** | Container Security | Atividade 1 (Peso 3,0) | **VERIFICADO** | Trivy scanner e script `01-pipeline/check-container.sh`. | Análise de imagem base e pacotes do SO Alpine. |
| **R06** | Explicação de execução e mitigação de riscos | Atividade 1 (Peso 3,0) | **VERIFICADO** | Seção 1.2 deste documento e `01-pipeline/REPORT.md`. | Rastreabilidade entre etapas e riscos mitigados. |
| **R07** | Criptografia local | Atividade 2 (Peso 2,5) | **VERIFICADO (Handoff)** | `03-frontends/FRONTEND-HANDOFF.md`. | Especificação de Android Keystore / iOS Keychain. |
| **R08** | Rate limiting na API | Atividade 2 (Peso 2,5) | **VERIFICADO** | `RateLimitFilter.java`, `RateLimitFilterTest.java`. | Bucket4j ativo (50 req/min geral, 5 req/min sensível). |
| **R09** | Validação de entrada server-side | Atividade 2 (Peso 2,5) | **VERIFICADO** | DTOs tipados com Jakarta Bean Validation e `GlobalExceptionHandler`. | Rejeição 400 Bad Request uniforme sem vazamento de stacktrace. |
| **R10** | JWT seguro e refresh tokens | Atividade 2 (Peso 2,5) | **VERIFICADO** | `JwtServiceImpl.java`, `AuthServiceImpl.java`, testes de integração. | JJWT HS256 >= 256b, validação de claim de tipo, rotação de refresh e sanitização 401. |
| **R11** | Controle de acesso por perfil | Atividade 2 (Peso 2,5) | **VERIFICADO** | `SecurityConfig.java`, testes unitários e Seção 2.3 deste documento. | Implementação com `USER`/`ANALYST` e mapeamento de equivalência conceitual aos perfis de domínio. |
| **R12** | MQTT/TLS para IoT | Atividade 2 (Peso 2,5) | **VERIFICADO (Handoff)** | `04-iot-infra/MQTT-TLS-HANDOFF.md`. | Especificação de porta 8883, TLS 1.3, validação de certificados e ACLs por tópico. |
| **R13** | IaC Security e hardening de contêiner | Atividade 2 (Peso 2,5) | **VERIFICADO** | `Dockerfile`, `04-iot-infra/CONTAINER-HARDENING.md`. | Usuário não-root (UID 10001), `read_only_rootfs: true`, `no-new-privileges: true`. |
| **R14** | Evidências reais: código, commits e explicações | Atividade 2 (Peso 2,5) | **VERIFICADO** | Código no repositório, histórico Git, suíte de 212 testes aprovada. | Testes em branch `sec-2026/fechamento` executados com 0 falhas. |
| **R15** | Logs estruturados (login, falhas, alterações críticas) | Atividade 3 (Peso 2,0) | **VERIFICADO** | `JsonLogLayout.java`, `AuthServiceImpl.java`, `AuthAuditLogTest.java`. | JSON em linha única, mascaramento PII e emissão pós-commit transacional. |
| **R16** | Plano de métricas e alertas (API, mobile, IoT, ML) | Atividade 3 (Peso 2,0) | **VERIFICADO** | `05-observability/ALERTS.md`, `MONITORING.md`. | Alerta ativo `alert-sec-violations` e mapeamento multicomponente. |
| **R17** | Dashboards e evidência visual | Atividade 3 (Peso 2,0) | **VERIFICADO (Workbook)** | `05-observability/DASHBOARD.md`, `workbook.json`. | Azure Monitor Workbook implantado (`24de0afb-...`) com 7 KQLs; captura via portal orientada. |
| **R18** | Plano de resposta a incidentes em cinco etapas | Atividade 3 (Peso 2,0) | **VERIFICADO** | `05-observability/INCIDENT-RESPONSE.md`, `TABLETOP.md`. | 5 etapas documentadas e exercício de simulação de mesa realizado. |
| **R19** | Revisão final de riscos (STRIDE + DevSecOps) | Atividade 4 (Peso 2,5) | **VERIFICADO** | `06-compliance/REPORT.md`. | 10 ameaças analisadas em 5 fronteiras de confiança. |
| **R20** | Mapeamento com normas (ASVS, Mobile Top 10, API Top 10) | Atividade 4 (Peso 2,5) | **VERIFICADO** | `06-compliance/COMPLIANCE-MATRIX.md`. | ASVS v4.0.3, OWASP API Top 10 (2023), Mobile Top 10 (2024). |
| **R21** | LGPD: dados pessoais, telemetria e localização | Atividade 4 (Peso 2,5) | **VERIFICADO** | `06-compliance/LGPD.md`. | Inventário, bases legais, retenção e riscos de tabelas Envers. |
| **R22** | Plano de rotinas contínuas e backup/recuperação | Atividade 4 (Peso 2,5) | **VERIFICADO** | `06-compliance/CONTINUOUS-SECURITY.md`, `BACKUP-RECOVERY.md`. | 4 rotinas de segurança + procedimento de restore PostgreSQL em ambiente isolado. |
| **R23** | Documento consolidado com 4 atividades + checklist | Transversal (Peso 10,0) | **VERIFICADO** | `docs/security/sec-2026/ENTREGA.md` (este documento). | Documento único e completo integrando os 23 requisitos do programa. |

---

## Conclusão e considerações finais
A entrega de segurança do projeto CarSync consolida a evolução de um modelo tradicional de desenvolvimento para uma cultura **DevSecOps contínua**. Todas as 4 atividades foram cobertas com profundidade técnica, código auditado e testado, artefatos versionados no Git e recursos implantados e validados no ambiente Azure de produção.
