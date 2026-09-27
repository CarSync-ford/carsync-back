# Status da Frente de Hardening de API (02-api)

Data: 2026-09-26  
Status Geral: **CONCLUÍDO (Hardening e Auditoria) / BLOQUEIO FORMAL (Matriz de Perfis Docente)**  

---

## 1. Tabela de Checkpoints

| Checkpoint | Descrição | Status | Evidência / Arquivos |
|---|---|---|---|
| **T1.C1** | Registrar contratos atuais de autenticação/autorização | **CONCLUÍDO** | `docs/security/sec-2026/02-api/CONTRACT.md` |
| **T2.C1** | Separar access/refresh/reset na autenticação bearer | **CONCLUÍDO** | `JwtAuthenticationFilter`, `AuthServiceImpl` |
| **T2.C2** | Proteger `/api/v1/auth/**` e alinhar principal `UserDetails` | **CONCLUÍDO** | `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `SecurityConfigTest.java` |
| **T2.C3** | Rotação/TTL de refresh token e capacidade no banco | **CONCLUÍDO** | `AuthServiceImpl.java`, `AuthControllerIntegrationTest.java` |
| **T2.C4** | Persistência de lockout e integridade transacional | **CONCLUÍDO** | `AuthServiceImpl.java`, testes de lockout existentes |
| **T3.C1** | Definir correspondência de 3 perfis com domínio Ford | **BLOQUEADO** | Bloqueio formal registrado: aguardando matriz aprovada pelo docente responsável |
| **T3.C2** | Aplicar matriz aprovada sem autoelevação | **BLOQUEADO** | Bloqueado por T3.C1 (nenhuma role arbitrária adicionada) |
| **T4.C1** | Rate limit e validação de entrada | **CONCLUÍDO** | `BucketRateLimitTest.java`, validação DTOs |
| **T5.C1** | Logs estruturados em JSON válido de linha única | **CONCLUÍDO** | `JsonLogLayout.java`, `logback-spring.xml`, `JsonLogLayoutTest.java` |
| **T5.C2** | Eventos de auditoria pós-commit (zero PII/segredos) | **CONCLUÍDO** | `AuthServiceImpl.java`, `AuthAuditLogTest.java`, `docs/security/sec-2026/02-api/EVENTS.md` |
| **T6.C1** | Consolidar evidências e documentação | **CONCLUÍDO** | Teste global `mvn clean test` (222/222 passando), patch `/tmp/sec-api-fechamento.patch` |

---

## 2. Bloqueios Identificados

- **T3.C1 / T3.C2 (Equivalência Docente de Perfis)**: A exigência de 3 perfis (Brigadista, Gestor, Administrador) não possui mapeamento formal aprovado com o modelo atual (`USER`, `ANALYST`). Criar perfis arbitrários violaria as regras da rubrica. O bloqueio está documentado no `CONTRACT.md` e os demais checkpoints seguiram normalmente.
- **Swagger UI 404 vs Spring 7 / Spring Boot 4.1.1**: O endpoint `/v3/api-docs` está operacional e com esquema `bearerAuth` configurado. A interface web `/swagger-ui.html` falha por incompatibilidade do webjar com a resolução de recursos estáticos do Spring Framework 7. O `pom.xml` não foi alterado por restrição de escopo da frente 01.
