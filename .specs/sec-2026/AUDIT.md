# Auditoria de cobertura — 2026-09-23

Base: `ceca50ded9c46d1dbc16c2ab1f17217055935e94`. Fonte normativa: `SEC-REQUIREMENTS.md`. Conclusão: as specs antigas **não cobrem exatamente o necessário**. Misturam obrigações, melhorias opcionais, itens parcialmente implementados e alegações de conformidade sem evidência.

## Método e limites

- Graphify existente na raiz consultado, 1.476 nós; consulta refinada com vocabulário `security pipeline compliance encryption mobile observability tls`. Resultado usado para localizar fontes, não provar implantação. Consultas truncadas não representam auditoria integral do grafo.
- Sete specs históricas lidas integralmente; fontes Java, testes, configuração, Dockerfile, workflow e documentação confrontados com o enunciado. Agentes em worktrees não encontraram o grafo não versionado; revisão desses agentes foi diretamente sobre os arquivos. Não houve rebuild do grafo.
- `mvn -B test -Dspring.profiles.active=test`: **153 testes, zero falhas, erros ou skips**, executados em worktree isolado da base durante esta auditoria. Suíte verde não prova cenários ausentes.
- Nenhum frontend oficial, broker IoT ou ambiente cloud foi executado/validado. Estado remoto, dashboards, alertas e backup não foram comprovados.
- Custo de tokens graphify: consultas locais determinísticas, sem chamada LLM de extração; custo total da sessão não apurado. Não atribuir custo zero à auditoria com agentes.

## Comparação com as specs antigas

| Spec histórica | Problema de cobertura | Destino mínimo |
|---|---|---|
| Phase 0 | Checks/histórico citam testes e resultados não representativos da base; HMAC habilitado com secret vazio não garante proteção | 02: verificar controles efetivos, sem duplicar implementação |
| Phase 1 | Mistura LGPD com políticas quantitativas e ampliações de produto; masking/retenção existem mas histórico auditado não prova eliminação | 06: mapear dados, limites e risco residual; 02 recebe correção necessária pelo protocolo |
| Phase 2 | MFA/reset/SMTP não são exigências textuais; auth parcialmente implementada contém falhas reais; integração front explicitamente delegada | 02: corrigir hardening existente; 03: testar integração no cliente |
| Phase 3 | Cobre scanners, mas workflow atual não os executa; receita tem `package-ecystem` incorreto | 01: pipeline, diagrama e evidência real, não copiar receita sem validação |
| Phase 4 | KQL busca nomes/campos não demonstrados; metas fixas de painéis/alertas não constam do enunciado; faltam sinais dos componentes externos | 05: fontes reais, plano API/mobile/IoT/ML, prints e resposta |
| Phase 5 | Declara controles não comprovados, omite Mobile Top 10, usa ausência IoT como N/A; conformidade ASVS superestimada | 06: checklist versionado e honesto; 03/04 fornecem evidências externas |
| Phase 6 | Implementação revertida por `c362a1c`; Redis/APIM/Cloudflare/escala não são obrigações da rubrica | Não recriar; somente rotinas normativas passam para 06 |

## Achados que fundamentam as novas specs

### Pipeline e infraestrutura

`.github/workflows/deploy.yml`: testes, build, push de imagem e deploy; sem SAST/SCA/secret/container scans. `Dockerfile` já usa usuário não-root e agente Application Insights. Isto justifica reuso e evidência, não construção de Terraform/Kubernetes ou novo provedor de segredos. `AZURE_SEC_CHANGES.md` é descrição, não consulta do estado cloud.

### JWT, autorização e autenticação

Fontes sob `src/main/java/br/com/sprint1/challenge/`:

- `config/JwtAuthenticationFilter.java`: aceita token assinado sem separar finalidade access/refresh/reset; role ausente recebe USER.
- `service/impl/JwtServiceImpl.java`: HS256, chave mínima de 32 **bytes**, expiração; issuer emitido, não exigido pelo parse atual.
- `config/SecurityConfig.java` e `controller/AuthController.java`: `/auth/**` público inclui operações autenticadas; `@SecurityRequirement` não impõe autorização. Controller espera UserDetails, filtro cria String.
- `service/impl/AuthServiceImpl.java`: lockout atualizado em transação que lança RuntimeException; confirmar persistência com teste integrado. Refresh usa prazo persistido fixo versus TTL configurável. Reset/MFA não são fluxos end-to-end concluídos.
- `controller/AnalyticsController.java` e `controller/UserController.java`: ANALYST/USER não demonstram automaticamente Brigadista/Gestor/Administrador. Definir matriz aprovada, sem inventar equivalência de domínio.
- `config/RateLimitFilter.java`: limite em memória por instância; confiança de headers de IP depende da fronteira real do ingresso. Teste deve registrar premissas; solução distribuída não é exigência automática.

Não transformar achados adicionais em novas funcionalidades. Corrigir falha ligada ao controle obrigatório e preservar controles existentes; itens fora da entrega continuam riscos visíveis, não requisitos inventados.

### Clientes, MQTT e observabilidade

A verificação frontend explicitamente citada é integração após mudança de auth, em `.specs/phase-2-auth-hardening.md`. **Não há evidência de requisito de certificate pinning.** Criptografia local é requisito separado e não é demonstrada por BCrypt, TLS ou banco cloud cifrado.

Exemplo mobile sob `test/ford-test-main/ford-test-main/FORDTESTE/fordretain/03_mobile/fordretain-app/services/api.ts` usa contrato distinto e AsyncStorage; sua relação com o frontend oficial precisa ser confirmada. Não alterá-lo por suposição.

MQTT/TLS não foi localizado nesta API; deve ser verificado no componente IoT do projeto, não descartado como N/A pela ausência neste repo.

`src/main/resources/logback-spring.xml` interpola mensagens/exceções num formato com aparência JSON sem garantir escape; ausência de evento explícito de login bem-sucedido. Eventos atuais incluem SECURITY_VIOLATION e ANALYTICS_ACCESS; não presumir campos ou nomes das consultas históricas. Logs válidos ficam com 02; consultas, plano, capturas e resposta com 05.

### Compliance

Envers, masking e retenção são insumos, não conformidade completa. `entity/User.java` auditada mantém campos sensíveis/históricos; `service/DataRetentionService.java` não comprova limpeza integral de dados auditados. LGPD deve considerar dados pessoais, telemetria e localização, com risco residual explícito.

Faltam evidências suficientes para STRIDE final, mapeamentos ASVS/Mobile/API Top 10, rotinas e documento consolidado. Plano de backup é exigido; não se pode afirmar restore executado sem registro real. As quatro atividades devem existir no documento final único, não somente em um conjunto de links.

## Resultado desta rodada

Criadas seis specs e protocolo compartilhado, com matriz R01–R23, donos exclusivos, commits por checkpoint, worktree obrigatório ao iniciar desenvolvimento e modelos sugeridos. Specs antigas preservadas e sinalizadas como históricas. Handoff frontend escrito para execução humana ou por agente.

Nenhum hardening implementado nesta rodada; nenhum deploy, commit ou push efetuado. Worktrees de implementação das seis specs ainda não criados: serão criados no início de cada frente. Worktrees temporários de auditoria não substituem esse requisito.
