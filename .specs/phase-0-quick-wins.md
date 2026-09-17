# Phase 0 — Baseline de hardening e IaC

**Estado:** controles presentes no código; validação atual e evidências pendentes.
**Origem:** `SEC-REQUIREMENTS.md:24–36` (API, IAC, CODE-EVIDENCE).
**Contrato:** `.specs/README.md`.

Não recriar HMAC, headers, CORS ou validação de JWT já existentes. Não tratar alegação antiga de "92 testes passaram" como resultado desta revisão. Nenhuma configuração de produção foi verificada pelas specs.

## BASE-1 — Conferir e evidenciar controles existentes [Low]

**Arquivos-alvo:**

- `src/main/java/br/com/sprint1/challenge/config/SecurityConfig.java`
- `src/main/java/br/com/sprint1/challenge/config/HmacSignatureFilter.java`
- `src/main/java/br/com/sprint1/challenge/config/RateLimitFilter.java`
- `src/main/java/br/com/sprint1/challenge/service/impl/JwtServiceImpl.java`
- `src/main/resources/application.yml`
- Testes correspondentes em `src/test/java/`.

Passos:

1. Conferir controle, configuração e teste existente de HMAC, headers, CORS, JWT e rate limit. Registrar símbolos atuais; números de linha antigos não são contrato.
2. Rodar testes existentes pertinentes, incluindo `SecurityHeadersIntegrationTest`, `SecurityConfigCorsIntegrationTest`, `JwtServiceImplIntegrationTest`, `HmacSignatureFilterTest` e `HmacSignatureFilterIntegrationTest`, após confirmar seus nomes no checkout.
3. Se um controle falhar, registrar falha e encaminhar correção pontual High vinculada a API; não desligar filtro para obter teste verde.
4. Registrar na atividade 2 de `docs/security/SEC-DELIVERY.md` código, comando/resultado e risco mitigado. HMAC é assinatura, não evidência de criptografia local. Contagem de caracteres não comprova entropia da chave JWT.

**Aceite:** controles existentes descritos sem novos mecanismos; resultados verificáveis e pendências de ambiente separadas.

## IAC-1 — Segurança do Dockerfile existente [High]

**Entradas:** `Dockerfile`, `.dockerignore` se existir, workflow de build/deploy e configuração realmente usada.

1. Verificar usuário de execução, imagem base, arquivos copiados, secrets, portas e necessidade real de privilégios.
2. Corrigir apenas problemas encontrados: execução não root quando suportada, exclusão de `.env`/chaves e arquivos desnecessários do contexto, ausência de credenciais em ARG/ENV/layers e permissões mínimas de arquivos.
3. Preservar build Java 21, inicialização, health check e acesso aos diretórios necessários. Não acrescentar Terraform/Kubernetes.
4. Construir imagem e testar inicialização com configuração de teste, nunca credenciais de produção. Conferir usuário e arquivos/configuração da imagem sem revelar segredos.
5. Se Docker não estiver disponível, registrar bloqueio; leitura estática não comprova execução do container.

**Aceite:** trecho/diff do Dockerfile, explicação e evidência de build/startup; scan de imagem fica em phase-3. Dockerfile existente torna demonstração de IaC aplicável, salvo mudança de arquitetura documentada.

## BASE-2 — Validação de ambiente e evidências [Low]

**Pré-condição:** acesso autorizado ao ambiente. Não provisionar nem alterar secrets nesta tarefa.

- Verificar presença das configurações exigidas pelo código sem imprimir valores secretos.
- Conferir headers na URL real e comportamento de startup com configuração válida.
- Registrar ambiente, data e resultado. Evidência local e evidência remota devem permanecer distintas.
- Capturar prints/trechos sanitizados na atividade 2. Não executar comandos com valor de segredo literal no histórico do shell.

**Aceite:** validação comprovada ou pendência explícita de acesso. Não considerar ACA/Cloudflare configurados apenas por menção nos documentos antigos.

## Checklist

- [ ] BASE-1: baseline conferido e testes registrados.
- [ ] IAC-1: Dockerfile revisado, correções necessárias e demonstração.
- [ ] BASE-2: evidências do ambiente disponível e bloqueios identificados.
