# Specs de segurança — escopo SEC-REQUIREMENTS

## Fonte de verdade

`SEC-REQUIREMENTS.md` define a entrega. Estas specs substituem o roadmap anterior de sete fases de maturidade. Numeração e nomes dos arquivos existentes foram mantidos para não quebrar referências; não representam ordem obrigatória de execução.

**Estado desta revisão:** planejamento atualizado, não implementação. Nenhum checkbox abaixo comprova código funcionando ou implantação. Não usar contagens antigas de testes como evidência atual.

As quatro atividades devem aparecer em **um único documento final**, `docs/security/SEC-DELIVERY.md`, com evidências reais. Arquivos auxiliares, configurações e testes podem existir; não substituem o documento consolidado.

## Rastreabilidade

| ID local | Requisito e linhas de SEC-REQUIREMENTS.md | Spec responsável | Evidência mínima |
|---|---|---|---|
| PIPE | Pipeline, SAST, SCA, secrets, container quando aplicável, riscos e diagrama — 4–18 | phase-3 | Diagrama, configuração e explicação da execução Ford |
| CRYPTO | Criptografia local — 24–25 | phase-8 | Código, teste e evidência de dados locais protegidos |
| API | Rate limit, validação de entrada, JWT seguro — 26–27 | phase-0, phase-2 | Testes positivos/negativos e trechos de código |
| RBAC | Brigadista, Gestor, Administrador — 28–29 | phase-1 | Matriz de acesso aprovada e testes 401/403/sucesso |
| IOT | MQTT/TLS — 30 | phase-8 | Configuração cliente/broker e testes de conexão segura |
| IAC | IaC Security, quando aplicável — 31–34 | phase-0 | Dockerfile/configuração real, justificativa e evidência |
| CODE-EVIDENCE | Código, prints, commits e explicações — 35–36 | phase-0, phase-1, phase-2, phase-8 | Referências verificáveis na atividade 2 |
| LOG | Logs estruturados de login, falhas e alterações críticas — 42–44 | phase-4 | Exemplos sanitizados de eventos reais |
| MON | Métricas, alertas de API/mobile/IoT/ML e dashboards — 45–47 | phase-4 | Consultas, alertas e prints reais |
| IR | Detecção, análise, contenção, erradicação, recuperação — 48–51 | phase-4 | Fluxo e resposta executável no ambiente existente |
| RISK | Revisão STRIDE + DevSecOps — 57 | phase-5 | Risco, controle, evidência e risco residual por componente |
| OWASP | ASVS, Mobile Top 10, API Top 10 — 58–61 | phase-5 | Mapeamento versionado, sem declarar conformidade não demonstrada |
| LGPD | Dados pessoais, telemetria e localização — 62 | phase-1, phase-5 | Inventário, finalidade/base legal a validar, controles e lacunas |
| CONT | Rotinas de dependências, testes, permissões, backup/recuperação — 63–67 | phase-5 | Frequência, responsável, procedimento e evidência esperada |
| FINAL | Documento único por atividade e checklist — 2, 68 | phase-5 | SEC-DELIVERY.md e checklist rastreável |
| CLEANUP | Remover funcionalidades excedentes já implementadas — pedido do usuário | phase-7 | Ausência das funções removidas e testes de regressão |

Os IDs desta tabela são apenas referências locais; não são novos requisitos.

## Limite de escopo

- Manter escolhas técnicas necessárias aos requisitos: autenticação básica, BCrypt/validação de senha, JWT/refresh seguro, proteção contra força bruta, validação de entrada, rate limit, CORS, headers e proteção de logs.
- Não retirar um controle seguro apenas porque a biblioteca ou técnica não está nomeada no enunciado. HMAC e TLS já existentes podem sustentar hardening; não criar nova frente de HMAC, WAF ou CDN.
- Remover recuperação de senha (`forgot-password`/`reset-password`, tokens e SMTP exclusivos), MFA/TOTP e papel ANALYST com seus caminhos exclusivos, conforme phase-7. Troca autenticada de senha não é recuperação de senha; deve permanecer segura.
- Retirar automações de retenção/anonimização com prazos arbitrários do roadmap e, se presentes, do runtime conforme phase-7. Manter minimização de dados, mascaramento útil e definição de retenção legal no mapeamento LGPD. Não apagar registros de negócio para reduzir escopo.
- Não planejar Redis, rate limiting distribuído, APIM, nova infraestrutura Cloudflare, dashboards comerciais, geolocalização de IPs, assinatura de imagens ou SBOM como entregas adicionais. Se já houver infraestrutura, preservar operação segura, sem desprovisionar por esta revisão.
- Não transformar mapeamento OWASP em projeto de certificação ASVS L1/L2 integral. Lacunas reais devem ser registradas; correções só entram quando rastreáveis ao enunciado.
- Ausência de mobile/IoT/ML neste repositório **não permite** marcar esses requisitos como N/A. Localizar os componentes Ford ou registrar bloqueio de integração, responsável e evidência pendente. Não criar aplicativos, modelos ML, serviços ou firmware fictícios.
- Apenas Container Security e IaC têm condicional explícita de aplicabilidade. Usar artefatos existentes; não introduzir Terraform/Kubernetes apenas para demonstrar IaC.

## Execução por Gemini 3.8 Flash High/Low

High/Low são os perfis de execução solicitados pelo usuário, não dependências da aplicação nem garantia de identificador disponível no provedor. Não adicionar SDK, integração Gemini ou configuração de inferência ao projeto.

Cada tarefa tem ID, perfil sugerido, entrada, ação e aceite. Executar uma tarefa por vez:

1. Ler `SEC-REQUIREMENTS.md`, este índice e apenas a tarefa escolhida. Conferir arquivos/símbolos no checkout atual antes de alterar.
2. Confirmar pré-condições. Se depender de permissão, política de negócio, repositório externo ou ambiente indisponível, registrar `BLOQUEADO` e motivo; não inventar decisões nem evidências.
3. Aplicar a menor mudança nos arquivos indicados. Reutilizar biblioteca instalada, recurso nativo ou stdlib; não criar abstrações para uso futuro.
4. Rodar teste específico da tarefa. Mudança de lógica exige teste executável no padrão já existente; não adicionar framework de testes.
5. Revisar diff e registrar resultado: arquivos alterados, comando, saída resumida, requisito atendido e pendência. Só marcar aceite após comprovação.

**Low:** documentação, configuração ou teste pontual com contrato já definido. **High:** autorização, criptografia, transações, migrações, remoção de fluxos ou integração entre componentes. Se uma tarefa Low exigir decisão de segurança não descrita, parar e encaminhar para High, sem ampliar escopo.

Regras comuns:

- Não editar/apagar migrações Flyway históricas. Migração nova usa próximo número livre verificado no momento; manter par PostgreSQL/H2 quando o projeto o exige.
- Não publicar deploy, mudar permissões de usuários, revogar sessões globais, apagar dados ou desprovisionar serviços sem autorização específica. Planejar transição antes de executar.
- Não armazenar senhas, tokens, seeds TOTP, chaves privadas ou PII em logs, prints, fixtures ou documento final.
- Nunca substituir teste negativo por remoção da proteção. Manter autenticação, validação de senha e negação por padrão durante mudanças de RBAC e auth.
- Resultado de teste local não prova ambiente de produção. Capturas simuladas devem ser identificadas como simulação, não como implementação concluída.

## Ordem recomendada

1. `phase-0`: conferir baseline e evidências existentes.
2. `phase-1` RBAC e `phase-7`: preparar transição de perfis e retirada segura dos excedentes.
3. `phase-2`: validar auth remanescente após remoções.
4. `phase-8`: criptografia local e MQTT/TLS nos componentes reais.
5. `phase-3` e `phase-4`: pipeline e observabilidade; podem avançar com dependências disponíveis.
6. `phase-5`: consolidar evidências e lacunas das quatro atividades.

`phase-6` registra cancelamento do antigo plano de escala; não é backlog executável.
