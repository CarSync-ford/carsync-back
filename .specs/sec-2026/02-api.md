# 02-api — hardening, perfis e eventos necessários

Modelo sugerido: **GPT 6 Astra**. Estado: PENDENTE. Requisitos: R08–R11, insumos R14–R15. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/02-api` no início do desenvolvimento.

## Base e limites

Controles existentes: `SecurityConfig`, `RateLimitFilter`, `JwtAuthenticationFilter`, `JwtServiceImpl`, Bean Validation, `AuthServiceImpl`; autorização USER/ANALYST. Não há demonstração dos três perfis exigidos. Filtro JWT não distingue finalidade de token. Controller espera `UserDetails`, filtro usa principal String. `/auth/**` público inclui operações que deveriam exigir autenticação. Lockout lança exceção de runtime dentro de transação: confirmar persistência antes de afirmar eficácia.

Escrita exclusiva: `src/main/**`, `src/test/**`, `docs/security/sec-2026/02-api/**`. Exclui dependências do `pom.xml` (dono 01). Esta frente é **única dona dos eventos produzidos pela API**, inclusive `logback-spring.xml`; 05 consome eventos, não altera Java/XML/YAML. Não mudar contratos HTTP incidentalmente.

## Contrato com 03 e 05

Preservar login `POST /api/v1/auth {email,password}` e refresh `POST /api/v1/auth/refresh {refreshToken}`, ambos retornando `{token,refreshToken}`. Mudança indispensável deve ser publicada em `CONTRACT.md` antes de implementação e consumida por revisão explícita do frontend, nunca inferida silenciosamente.

Eventos mínimos para R15: login bem-sucedido, falha de autenticação e alteração crítica real (por exemplo troca de senha). Usar codificação JSON com escape correto. Publicar exemplo sanitizado e campos disponíveis em `EVENTS.md`; não prometer campo IP/usuário/correlação inexistente, nem registrar senha, JWT, refresh token ou secret MFA. 05 pode elaborar consultas sem aguardar, mas só as valida contra esses eventos reais.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Registrar contratos atuais, matriz de rotas/perfis e testes disponíveis | `CONTRACT.md`, riscos confirmados vs hipóteses separados; contratos comparados a DTO/controller | `docs(sec-api): freeze authentication and authorization contracts` |
| T2.C1 | Separar access/refresh/reset na autenticação bearer | Teste aceita access válido; rejeita refresh/reset como bearer, assinatura adulterada e expirado; validar subject/claims necessários | `fix(sec-api): enforce access token purpose` |
| T2.C2 | Proteger operações autenticadas em `/auth/**` e alinhar tipo do principal | Testes sem token rejeitado, identidade válida recebida, ausência de NPE/500; login/refresh públicos continuam funcionais | `fix(sec-api): secure authenticated auth operations` |
| T2.C3 | Verificar rotação/TTL de refresh existentes; corrigir defeito confirmado | Testes replay de refresh rejeitado e expiração persistida coerente com configuração; se forem dois defeitos independentes, C3a rotação e C3b TTL, cada um com teste e commit próprios | `fix(sec-api): preserve refresh rotation and expiry guarantees` |
| T2.C4 | Verificar persistência do lockout existente; corrigir rollback confirmado | Teste integrado de falhas e bloqueio persistindo após término da transação | `fix(sec-api): persist failed login and lockout state` |
| T3.C1 | Definir correspondência dos três perfis com o domínio Ford | Matriz Brigadista/Gestor/Administrador versus USER/ANALYST e rotas; equivalência só com justificativa aceita pelo responsável; não declarar dois papéis equivalentes a três sem demonstração | `docs(sec-api): define required profile permission matrix` |
| T3.C2 | Aplicar matriz aprovada sem autoelevação no cadastro público | Testes parametrizados de permitido/negado para cada perfil, não autenticado e cadastro sem atribuição privilegiada; migration se necessária preserva dados | `fix(sec-api): enforce required role permissions` |
| T4.C1 | Evidenciar rate limit e validação de entrada existentes; corrigir somente falha comprovada | `mvn test` direcionado: limite produz 429, payload inválido produz 400, válido mantém contrato; explicar limite por instância | `test(sec-api): cover rate limiting and input rejection` |
| T5.C1 | Garantir logs estruturados válidos | Parse de linhas com aspas/quebras/exceções como JSON; teste não expõe segredos; sem nova stack observability | `fix(sec-api): emit valid structured security logs` |
| T5.C2 | Completar eventos de login, falha e alteração crítica | Disparar três eventos em ambiente de teste, registrar amostras sanitizadas em `EVENTS.md`, teste/regressão por evento | `feat(sec-api): record required security audit events` |
| T6.C1 | Consolidar evidências e avisos aos consumidores | Testes focados e `mvn test`; relatório de resultados reais, contrato inalterado ou alteração explicitada | `docs(sec-api): record hardening evidence and client contract` |

## Aceite e bloqueios

Testar pelo comportamento HTTP/integrado onde possível, usando stack de testes existente. Não marcar teste apenas escrito como executado. Registrar falhas de infraestrutura separadamente. Perfil/matriz sem definição de domínio é bloqueio de T3.C2, não autorização para inventar permissões amplas; demais checkpoints seguem assíncronos.

Não exigir completar produto MFA/reset/email, trocar algoritmo JWT, implantar Redis/APIM nem adicionar recursos LGPD fora da rubrica. Endpoints já expostos não podem ficar inseguros por esse limite: corrigir proteção existente; comportamento incompleto deve ser explicitado ao frontend e responsável, sem anunciar garantia que não existe.

Saídas: `REPORT.md`, `CONTRACT.md`, `EVENTS.md`, `STATUS.md` na pasta exclusiva; trechos/testes/commits são insumos R14 para 06.
