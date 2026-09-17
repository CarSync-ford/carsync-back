# Phase 2 — Validar hardening de API sem funcionalidades excedentes

**Estado:** lockout e rotação de refresh presentes no código; revalidar após remoções da phase-7. Recuperação de senha e MFA também estão implementados, mas devem ser retirados, não expandidos.
**Origem:** `SEC-REQUIREMENTS.md:26–27` (API).
**Contrato:** `.specs/README.md`.

## Escopo preservado

Login, autenticação JWT, refresh seguro, troca autenticada de senha existente, BCrypt/validação de senha, proteção contra força bruta, rate limit e validação de entrada. Lockout e rotação são escolhas técnicas já existentes para hardening, não novas funcionalidades a construir.

**Fora do alvo:** forgot/reset password, tokens PASSWORD_RESET, SMTP exclusivo de recuperação e MFA/TOTP. Retirada detalhada em phase-7. Não criar novo mecanismo de recuperação ou bypass administrativo de senha como substituto.

## API-1 — Regressão de autenticação e tokens [High]

**Entradas/arquivos-alvo:** `AuthController`, `AuthDtos`, `AuthService`, `AuthServiceImpl`, `JwtServiceImpl`, `JwtAuthenticationFilter`, `JwtProperties`, `UserRepository` e testes correspondentes.
**Pré-condição para aceite final:** CLEAN-1 e CLEAN-2 concluídos.

1. Preservar login com credenciais válidas e rejeição genérica de credenciais inválidas, sem enumeração de usuários.
2. Validar assinatura, expiração e tipo/finalidade de JWT. Access token não pode substituir refresh; antigo token PASSWORD_RESET não pode autenticar API nem renovar sessão após retirada do reset.
3. Preservar rotação single-use de refresh, expiração consistente com configuração e serialização concorrente. Não introduzir outro prazo/configuração de 30 dias divergente do TTL atual.
4. Preservar persistência de falhas e bloqueio após cinco tentativas por quinze minutos, caso esses sejam os valores confirmados no código; não refazer lockout nem adicionar job de desbloqueio desnecessário.
5. Preservar troca autenticada de senha com senha atual válida, política forte e revogação de refresh. Não alegar revogação imediata de access token sem mecanismo real.
6. Adaptar apenas testes afetados pelas remoções, mantendo cobertura dos controles remanescentes.

**Testes de referência:** `AuthServiceLockoutIntegrationTest`, `AuthServiceRefreshIntegrationTest`, `AuthTokenErrorIntegrationTest`, `JwtAuthenticationFilterTest`, `JwtServiceTest`, `JwtServiceImplValidationTest`, `AuthServiceTest`. Confirmar nomes atuais antes de executar.

**Aceite:** casos positivos, negativos e concorrentes passam; auth não depende de SMTP/TOTP; nenhum token legado de reset ganha acesso genérico.

## API-2 — Rate limit e validação de entrada [High]

**Entradas:** `RateLimitFilter`, DTOs/Bean Validation, tratamento global de exceções e `RateLimitFilterTest`.

1. Conferir limites reais, rotas cobertas, identificação do cliente e resposta 429. Headers encaminhados só podem ser confiados conforme proxy real; não aceitar IP arbitrário do cliente para contornar limite.
2. Testar burst acima do limite e requisição válida permitida. Preservar diferenciação existente de rotas sensíveis quando útil.
3. Documentar rate limit por instância e topologia usada na demonstração. Não afirmar limite global em múltiplas réplicas nem adicionar Redis/APIM preventivamente.
4. Escolher DTOs reais de autenticação e entrada de negócio e demonstrar rejeição de campos ausentes/inválidos, limites e payload inválido, sem stack trace ou dados sensíveis na resposta.
5. Corrigir apenas gaps necessários aos controles exigidos; CORS não substitui validação de payload.

**Aceite:** testes demonstram 429 e rejeições 400 consistentes com contrato; chamadas válidas continuam funcionando; limitações de implantação registradas.

## API-3 — Evidências [Low]

**Dependência:** API-1, API-2 e baseline phase-0.
**Arquivo-alvo:** atividade 2 de `docs/security/SEC-DELIVERY.md`.

Registrar controles, trechos de código, testes/comandos, prints sanitizados quando pertinentes e commits existentes. Explicar diferença entre hardening preservado e funcionalidades retiradas. Executar `mvn test` ao final da integração; registrar falhas, nunca afirmar suíte verde sem execução.

**Aceite:** rate limit, validação e JWT seguro têm evidências individuais, não apenas lista de bibliotecas.

## Checklist

- [ ] API-1: login/JWT/refresh/lockout/troca autenticada sem regressão.
- [ ] API-2: rate limit e validação demonstrados.
- [ ] API-3: evidências reais consolidadas.
- [ ] Reset/SMTP e MFA ausentes conforme phase-7; migrações históricas preservadas.
