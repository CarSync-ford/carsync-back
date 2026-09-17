# Phase 7 — Retirar implementação excedente

**Estado:** nova spec; nenhuma remoção de código executada nesta revisão.
**Origem:** pedido explícito de limitar implementação a `SEC-REQUIREMENTS.md`, incluindo remoção de recuperação de senha.
**Contrato:** `.specs/README.md`. Todas as tarefas de remoção são **High**; não executar por simples substituição textual.

## Fronteira da remoção

Retirar reset de senha/SMTP exclusivo, MFA/TOTP, papel ANALYST e endpoints adicionados exclusivamente para ele, além dos jobs de retenção com prazos arbitrários. Preservar funcionalidades de negócio preexistentes, autenticação básica, troca autenticada de senha, validação, JWT, lockout, refresh, minimização de dados e trilha de auditoria necessária.

Presença de arquivo de migração não prova aplicação em produção. Conferir histórico Flyway do ambiente autorizado antes de qualquer mudança de schema. Nunca editar ou apagar migrações históricas V1–V12; confirmar sequência atual antes de criar novas.

## CLEAN-0 — Preparar corte e dependências [High]

**Entradas:** referências dos símbolos abaixo, configurações, testes, clientes e schema atual.

1. Inventariar chamadas de clientes aos endpoints a retirar e contas com MFA/papéis antigos, sem exportar PII ou segredos.
2. Separar alteração local de rollout. Remover MFA reduz proteção de contas atualmente inscritas; comunicar impacto e obter autorização específica de corte no ambiente antes de desativar.
3. Definir ordem de deploy e schema: remover dependências do runtime primeiro; limpeza de colunas depois, quando nenhuma versão antiga estiver ativa. Não implantar versão antiga contra schema incompatível como rollback.
4. Não mudar dados/credenciais em produção nesta preparação. Registrar bloqueios de acesso e dependências externas.

**Aceite:** lista de consumidores/estados afetados e plano de transição; ações irreversíveis claramente separadas.

## CLEAN-1 — Remover recuperação de senha e SMTP exclusivo [High]

**Arquivos-alvo**, relativos a `src/main/java/br/com/sprint1/challenge/`:

- `controller/AuthController.java`: `forgotPassword`, `resetPassword`.
- `dto/AuthDtos.java`: `ForgotPasswordRequest`, `ResetPasswordRequest`.
- `service/AuthService.java`, `service/impl/AuthServiceImpl.java`: métodos e helpers exclusivos de reset.
- `service/PasswordResetEmailService.java`, `service/impl/SmtpPasswordResetEmailService.java`.
- `entity/User.java`: estado de reset; conferir acessos por getters/setters.
- `config/SecurityConfig.java`, `config/JwtAuthenticationFilter.java`, `config/RateLimitFilter.java`: tratamento de rotas/tokens a retirar.
- `pom.xml`, `src/main/resources/application.yml`, `src/test/resources/application.yml`: dependência/configuração mail exclusivamente usada por reset.
- Testes de auth/reset/email e configurações de ambiente/documentação que referenciem SMTP ou reset.

Passos:

1. Remover handlers `POST /api/v1/auth/forgot-password` e `/reset-password`, DTOs e serviço de envio. Não manter stubs 202 nem modo mock.
2. Remover geração/consumo de token PASSWORD_RESET, helpers e estado de entidade exclusivos. Preservar validação genérica de finalidade/tipo dos JWTs.
3. Retirar rotas da allowlist pública e exceções em filtros. Não ampliar allowlist para fazer testes passarem.
4. Retirar `spring-boot-starter-mail`, `spring.mail`, `SMTP_*` e propriedades de reset somente após confirmar ausência de outro consumidor. Se compartilhados, remover apenas parte exclusiva e registrar motivo.
5. Remover testes de funcionalidades eliminadas (`AuthServicePasswordResetIntegrationTest`, `PasswordResetEmailServiceTest` após conferir conteúdo) e adaptar testes mistos, especialmente `AuthPasswordValidationIntegrationTest`, preservando validação da troca autenticada de senha.
6. Adicionar regressão: rotas sem handler/OpenAPI; requisição não executa reset; aplicação inicia sem configuração SMTP; token antigo PASSWORD_RESET é rejeitado pela API e pelo refresh.

**Aceite:** nenhum endpoint/serviço de recuperação ou envio SMTP exclusivo no runtime; troca autenticada continua exigindo senha atual e senha nova forte. Ausência de handler deve ser verificada no mapping/OpenAPI; filtro pode responder 401/403 antes de 404, portanto 401 sozinho não prova remoção.

## CLEAN-2 — Remover MFA/TOTP [High]

**Pré-condição:** CLEAN-0; aprovação de corte para ambientes com usuários inscritos.
**Arquivos-alvo:** `AuthController`, `AuthDtos`, `AuthService`, `AuthServiceImpl`, `User`, `util/TotpUtil.java` e testes relacionados.

1. Remover `/api/v1/auth/mfa/enable`, `/verify`, `/disable`, DTOs `MfaEnableResponse`, `MfaVerifyRequest`, `MfaDisableRequest` e operações correspondentes.
2. Remover campo `code` de `AuthRequest`, construtor auxiliar exclusivo de MFA e lógica TOTP do login. Não remover validação de email/senha, lockout ou controle transacional.
3. Remover `TotpUtil` e teste exclusivo `TotpUtilTest` apenas após ausência de consumidores; implementação atual não exige adicionar/remover biblioteca TOTP por suposição.
4. Remover referências de entidade a `mfa_secret`, `mfa_enabled`, `mfa_last_used_step`; limpeza física em CLEAN-5.
5. Adaptar `AuthServiceTest` e testes mistos de login, preservando casos não MFA.
6. Verificar ausência dos endpoints no mapping/OpenAPI, login válido sem TOTP e credenciais inválidas rejeitadas. Não deixar flag de MFA com bypass silencioso como solução permanente.

**Aceite:** lifecycle e cálculo TOTP ausentes; login/refresh/lockout seguros conforme phase-2. Não registrar seeds nem preservá-las em evidências.

## CLEAN-3 — Retirar ANALYST e caminhos exclusivos [High]

**Pré-condição:** matriz/transição RBAC-1 aprovada. Executar junto de RBAC-2.
**Arquivos-alvo:** `controller/AnalyticsController.java`, `service/AnalyticsService.java`, `service/impl/AnalyticsServiceImpl.java`, `dto/AnalyticsDtos.java`, `util/DataMasker.java`, perfil/usuário, fixtures e `AnalyticsSecurityIntegrationTest`.

1. Preservar endpoints de negócio `/api/v1/analytics/overview` e `/service-share`, aplicando matriz aprovada dos três perfis. Não simplesmente apagar `@PreAuthorize` da classe e deixar acesso genérico autenticado.
2. Retirar listagens `/api/v1/analytics/customers`, `/leads`, `/vehicles` criadas para ANALYST, respectivos métodos `getCustomersAnalytics`, `getLeadsAnalytics`, `getVehiclesAnalytics` e DTOs `CustomerAnalyticsView`, `LeadAnalyticsView`, `VehicleAnalyticsView`, após confirmar consumidores e origem no histórico.
3. Se algum desses símbolos tiver uso preexistente necessário ao negócio, preservar esse uso protegido e retirar somente caminho exclusivo de ANALYST; registrar evidência da dependência, sem expandir API.
4. Remover `DataMasker` e testes apenas se ficarem sem consumidores. Não substituir dados mascarados por PII em respostas preservadas; minimização é controle útil ao requisito LGPD.
5. Eliminar referências ativas a ANALYST em autorização, seeds atuais, fixtures e documentos de uso; migrações históricas permanecem intactas.
6. Reaproveitar testes de segurança para provar matriz dos três perfis e ausência das rotas retiradas. Não apagar toda cobertura de autorização.
7. Migração de usuários/papel segue RBAC-2. Não excluir `user_type` ainda referenciado, apagar usuários ou conceder perfil privilegiado automaticamente.

**Aceite:** ANALYST não é perfil funcional no estado final; dois endpoints de negócio preservados com proteção; listagens excedentes removidas sem expor PII; tokens legados não burlam RBAC.

## CLEAN-4 — Retirar jobs/prazos arbitrários de retenção [High]

**Arquivos-alvo:** `service/DataRetentionService.java`, `config/DataRetentionProperties.java`, configuração de scheduling (confirmar `SchedulingConfig` e outros consumidores), propriedades `data-retention`, `DataRetentionServiceIntegrationTest` e métricas exclusivas.

1. Conferir consumidores e política aprovada existente. Se houver obrigação legal/operacional real atendida por algum trecho, registrar bloqueio e revisar escopo com responsável, não interromper descarte legal silenciosamente.
2. Retirar os dois jobs automáticos, propriedades de 30 dias/5 anos e counters exclusivos `data_retention.removed`. Não executar job como parte da remoção.
3. Remover habilitação de scheduling apenas se nenhum outro job a usar. Remover utilitários/beans de clock apenas se exclusivos.
4. Preservar `deleted_at`, semântica de exclusão, filtros e dados históricos até comprovar que não têm uso necessário. Retirar jobs não pode restaurar visibilidade de registros apagados logicamente.
5. Substituir testes exclusivos dos jobs por teste focado de ausência do agendamento e preservação da invisibilidade de registros excluídos onde houver esse contrato.
6. Política de retenção/descarte fica documentada no inventário LGPD, sem afirmar conformidade apenas porque automação foi removida.

**Aceite:** jobs excedentes não rodam; startup sem suas propriedades; dados preservados e nenhuma reexposição de registros.

## CLEAN-5 — Limpeza incremental de schema e referências [High]

**Pré-condição:** CLEAN-1/2 implementados e nenhuma versão ativa depende das colunas; transição de produção autorizada quando aplicável.
**Arquivos-alvo:** nova migração PostgreSQL e par H2, após última versão real; mapeamentos Envers e testes de schema quando afetados.

1. Conferir nomes/uso das colunas de reset (`password_reset_token_hash`, `password_reset_token_expires_at`) e MFA (`mfa_secret`, `mfa_enabled`, `mfa_last_used_step`). Não assumir tabela `password_reset_tokens`: implementação observada usa colunas em `users`.
2. Criar migração aditiva ao histórico para retirar somente colunas operacionais obsoletas. Verificar impacto em constraints, índices e auditoria; não apagar trilha histórica de negócio por conveniência.
3. Não reutilizar números V6–V12, nem editar checksum antigo. Confirmar SQL real; não inferir duplicação de coluna por resultado de grep concatenado.
4. Testar banco novo e upgrade de schema anterior com dados sintéticos, incluindo usuários com MFA/reset antigos. Preservar senha, usuário, lockout e refresh conforme transição definida.
5. Revogar/remover secrets SMTP externos apenas depois de confirmar uso exclusivo e obter autorização; não imprimir valores. Configuração local retirada não significa secret cloud removido.

**Aceite:** histórico antigo intacto; inicialização/migrações e integridade verificadas. Drops em produção ficam pendentes até autorização e corte seguro; não ocultar essa pendência como limpeza concluída.

## CLEAN-6 — Regressão e documentação [Low]

1. Executar testes afetados de auth, autorização e retenção; depois `mvn test`.
2. Buscar referências restantes a reset, SMTP, TOTP, ANALYST e jobs. Classificar referências históricas/testes negativos em vez de exigir zero ocorrências textual.
3. Atualizar documentação pública/checklist existente apenas onde anuncia funcionalidades retiradas, preservando alterações locais do usuário.
4. Registrar remoções e controles preservados na atividade 2 de `docs/security/SEC-DELIVERY.md`.

**Aceite:** diff restrito ao escopo; resultados reais registrados; nenhum controle básico enfraquecido por remoção acidental. Nenhum commit/deploy automático.

## Checklist

- [ ] CLEAN-0: corte e dependências identificados.
- [ ] CLEAN-1: recuperação e SMTP exclusivo retirados.
- [ ] CLEAN-2: MFA/TOTP retirado com transição segura.
- [ ] CLEAN-3: ANALYST/listagens exclusivas retirados e RBAC preservado.
- [ ] CLEAN-4: jobs arbitrários retirados sem perda/reexposição de dados.
- [ ] CLEAN-5: schema atualizado sem reescrever histórico.
- [ ] CLEAN-6: regressão e documentação coerentes.
