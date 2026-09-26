# Relatório de riscos — STRIDE e DevSecOps

- **Data da análise:** 2026-09-26
- **Escopo do sistema:** Solução CarSync (Ford Challenge) — API REST Spring Boot 4.1 / Java 21, Banco de Dados Relacional PostgreSQL (com migrações Flyway e Hibernate Envers), Frontend Web / Mobile, Telemetria IoT veicular e Modelos de Machine Learning (Churn Prediction).
- **Metodologia:** STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege) integrado ao ciclo de vida DevSecOps.

---

## 1. Ativos, fronteiras de confiança e fluxo de dados

### 1.1 Inventário de ativos críticos
1. **Credenciais e tokens de acesso:** Senhas (hash BCrypt), tokens JWT de acesso (HS256 >= 256 bits, TTL 15 min), Refresh Tokens (UUID com rotação em banco, TTL 7 dias), segredos TOTP de MFA (Base32), chaves de assinatura HMAC (`X-HMAC-Signature`).
2. **Dados cadastrais e pessoais (LGPD):** Clientes, usuários, condutores, emails, CPFs, telefones e registros de endereço.
3. **Telemetria e dados operacionais de veículos:** Quilometragem, status de garantia, histórico de ordens de serviço, alertas de peças e telemetria enviada via canais veiculares/IoT.
4. **Inteligência de negócio / ML:** Scores de predição de churn (risco de cancelamento), histórico de interações do assistente veicular e métricas de conversão de leads.
5. **Infraestrutura e pipeline:** Código-fonte (GitHub), imagens de contêiner (Azure Container Registry - ACR), ambiente de execução (Azure Container Apps - ACA), telemetria e logs (Azure Log Analytics / Application Insights).

### 1.2 Fronteiras de confiança (Trust Boundaries)
- **TB-1 (Cliente Público ↔ API Gateway/Ingress):** Internet pública para a API (`api.carsync.me`). Protocolo HTTPS (TLS 1.3 / 1.2).
- **TB-2 (API ↔ Banco de Dados):** Rede interna Azure / Docker network para PostgreSQL. Comunicação autenticada via credenciais injetadas por variáveis de ambiente seguras.
- **TB-3 (Dispositivos IoT Veiculares ↔ Broker MQTT):** Comunicação de telemetria veicular para broker (Mosquitto/HiveMQ) via TLS na porta 8883.
- **TB-4 (Aplicativo Mobile / Web ↔ Armazenamento Local):** Fronteira entre o código da aplicação e o armazenamento no dispositivo do usuário (Android Keystore / iOS Keychain).
- **TB-5 (Desenvolvedor / CI ↔ Produção):** GitHub Actions interagindo com Azure Container Registry e Azure Container Apps via Service Principal / OIDC federado.

---

## 2. Matriz de análise de ameaças STRIDE

| ID | Categoria STRIDE | Ativo / Fronteira | Ameaça identificada | Controle / Mitigação implementada | Evidência / Verificação | Risco residual | Responsável |
|---|---|---|---|---|---|---|---|
| **R-S01** | **Spoofing** | Tokens JWT / TB-1 | Falsificação de identidade via token adulterado ou token de tipo incorreto (ex: usar refresh token em rota de API). | JJWT com validação criptográfica estrita da assinatura HS256, checagem de claim de tipo (`ACCESS`), expiração obrigatória e rejeição de tokens expirados/revogados. | Testes em `JwtAuthenticationFilter`, `AuthServiceImpl` e `AuthControllerIntegrationTest`. | Baixo. Chave mestra mantida em variável de ambiente segura no Azure. | Equipe Backend / Sec |
| **R-S02** | **Spoofing** | Dispositivos IoT / TB-3 | Dispositivo não autorizado conectando-se ao broker MQTT fingindo ser um veículo Ford. | Conexão estrita TLS 1.3 / mTLS com validação de certificados X.509 ou credenciais de dispositivo com escopo individualizado por VIN. | Especificação e teste de rejeição documentados em `04-iot-infra/MQTT-TLS-HANDOFF.md`. | Médio. Handoff de infraestrutura com dependência de certificados nos dispositivos embarcados reais. | Equipe IoT / Firmware |
| **R-T01** | **Tampering** | Payloads de requisição / TB-1 | Adulteração de dados em trânsito entre clientes internos/parceiros e a API. | Assinatura HMAC-SHA256 via cabeçalho `X-HMAC-Signature` obrigatório para rotas de negócio protegidas (`HmacSignatureFilter`), somado a TLS em trânsito. | `HmacSignatureFilter`, validação em 23 rotas de produção, testes unitários e de integração. | Baixo. Requer sincronia de secret compartilhado entre cliente e API. | Equipe Backend |
| **R-T02** | **Tampering** | Dados cadastrais / TB-2 | Alteração indevida de dados sem rastreabilidade no banco de dados. | Auditoria via Hibernate Envers (`@Audited`) registrando histórico de revisões com timestamp e autor da alteração. | Entidades mapeadas com Envers, tabela `revinfo` gerada via Flyway. | Baixo. Modificações diretas de DBA no banco contornam aplicação; mitigado por RBAC restrito no Azure. | Equipe Dados / DB |
| **R-R01** | **Repudiation** | Ações de autenticação e transações / TB-1 | Usuário ou operador negar alteração de senha, tentativa de login maliciosa ou ativação de MFA. | Logs de auditoria estruturados em JSON único (`JsonLogLayout`) disparados estritamente pós-commit transacional (`afterCommit`), registrando ação, usuário correlacionado e resultado. | `AuthServiceImpl`, `AuthAuditLogTest`, logs em `appi-carsync-dev`. | Baixo. Telemetria encaminhada para Log Analytics imutável por retenção contratual. | Equipe Backend / Ops |
| **R-I01** | **Information Disclosure** | Armazenamento local Mobile / TB-4 | Extração de tokens JWT ou dados de perfil a partir do sistema de arquivos de dispositivo perdido/roubado. | Armazenamento cifrado via Android Keystore (MasterKey/EncryptedSharedPreferences) e iOS Keychain. | Especificação detalhada em `03-frontends/FRONTEND-HANDOFF.md`. | Médio. Dependência de implementação final no repositório mobile do cliente. | Equipe Mobile |
| **R-I02** | **Information Disclosure** | Logs operacionais e traces / TB-1 | Exposição de credenciais, senhas, tokens de reset ou PII em logs de aplicação e dashboards. | Mascaramento regex em runtime no `JsonLogLayout`, sanitização no `GlobalExceptionHandler` e exclusão de colunas `Message`, `Url` bruta e IP nas consultas do Workbook Azure. | `JsonLogLayoutTest`, `docs/security/sec-2026/05-observability/workbook.json`. | Baixo. Regras ativas em logback e consultas KQL do Azure. | Equipe Backend / Observability |
| **R-I03** | **Information Disclosure** | Vulnerabilidades em dependências / TB-5 | Inclusão de bibliotecas com CVEs críticas conhecidas de vazamento de dados (ex: Log4Shell, Jackson deserialization). | Scanner SCA (OWASP Dependency-Check) e Dependabot integrados no pipeline CI/CD com bloqueio automático de build para CVSS >= 7.0. | Relatórios de execução do workflow GitHub Actions e `01-pipeline/REPORT.md`. | Baixo. Varredura contínua a cada push e PR na main. | Equipe DevSecOps |
| **R-D01** | **Denial of Service** | Endpoints de autenticação e API / TB-1 | Ataques de força bruta em senhas ou sobrecarga volumétrica nos endpoints públicos. | Rate limiting nativo em memória via Bucket4j (`RateLimitFilter`) configurado com limites estritos por IP e bloqueio de conta após 5 falhas consecutivas de login. | `RateLimitFilterTest`, `AuthServiceImplTest`, testes de regressão da API. | Baixo. Em escala multirregional exigirá rate limiting distribuído (ex: Redis / Azure Front Door). | Equipe Backend / Infra |
| **R-D02** | **Denial of Service** | Contêiner Docker em produção / TB-5 | Esgotamento de recursos do host ou escape de privilégios a partir de contêiner comprometido. | Hardening de contêiner: execução sob usuário não privilegiado (`appuser:10001`), sistema de arquivos de leitura estrita (`read_only_rootfs: true`), bloqueio de elevação de privilégios (`no-new-privileges: true`), scanning com Trivy. | `Dockerfile`, `01-pipeline/check-container.sh`, `04-iot-infra/CONTAINER-HARDENING.md`. | Baixo. Contêiner atende às diretrizes CIS Benchmark. | Equipe DevSecOps |
| **R-E01** | **Elevation of Privilege** | Endpoints administrativos e analíticos / TB-1 | Usuário comum (`ROLE_USER`) acessar métricas corporativas ou endpoints de gestão sem autorização. | Autorização estrita baseada em perfis (`@PreAuthorize("hasRole('ANALYST')")`), validação de equivalência aos perfis de domínio (Brigadista, Gestor, Administrador) e bloqueio anônimo em `SecurityConfig`. | `SecurityConfig`, `SecurityConfigTest`, testes de integração de controle de acesso. | Baixo a Médio. Aguarda validação formal da equivalência de nomenclatura com o docente. | Equipe Backend / Produto |

---

## 3. Integração com o ciclo de vida DevSecOps

A segurança foi modelada e aplicada em todas as etapas do ciclo de engenharia:

```
[PLAN] ──> [CODE] ──> [BUILD/TEST] ──> [RELEASE/DEPLOY] ──> [OPERATE/MONITOR]
  │           │              │                 │                    │
  ▼           ▼              ▼                 ▼                    ▼
STRIDE     Lombok        Semgrep (SAST)    Trivy (Container)    Azure Monitor Workbook
LGPD       Validation    OWASP SCA         ACR push restrito    Log Analytics (KQL)
ASVS v4    HMAC / JWT    Gitleaks (Secrets) ACA deployment      Alertas automatizados
Threats    Bucket4j      212 testes JUnit  Env vars seguras     Runbooks de Resposta
```

1. **Planejamento (Plan):** Modelagem de ameaças STRIDE e definição dos critérios de aceitação alinhados a OWASP ASVS v4.0.3 e LGPD antes da escrita do código.
2. **Desenvolvimento (Code):** Boas práticas de codificação segura: validação server-side com Bean Validation (`@Valid`, `@NotNull`, `@Size`), HMAC SHA-256 para integridade, isolamento de senhas via BCrypt, mascaramento nativo de PII.
3. **Build e Testes (Build/Test):** Pipeline automatizado no GitHub Actions disparando:
   - **SAST:** Semgrep analisando regras de segurança OWASP Top 10 e padrões Java/Spring.
   - **SCA:** OWASP Dependency-Check inspecionando dependências do Maven e falhando em vulnerabilidades altas/críticas.
   - **Secret Scanning:** Gitleaks bloqueando credenciais, chaves privadas ou tokens no histórico do Git.
   - **Testes automatizados:** Suíte com mais de 212 testes unitários e de integração validando regras de segurança e fluxos de negócio.
4. **Deploy e Liberação (Release/Deploy):** Construção de imagem Docker multi-stage com hardening de segurança (non-root `appuser:10001`), varredura de vulnerabilidades com Trivy e deploy em Azure Container Apps via credenciais seguras.
5. **Operação e Monitoramento (Operate/Monitor):** Monitoramento contínuo em produção através do Azure Monitor Workbook `CarSync — Segurança e Operação`, Log Analytics com consultas KQL estruturadas, regras de alerta (`alert-sec-violations`) e procedimentos formais de resposta a incidentes.
