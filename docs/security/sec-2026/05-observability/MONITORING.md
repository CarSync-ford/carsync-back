# Plano de monitoramento — API, mobile, IoT e ML

**Spec:** `05-observability` · **base:** `41d2227eb7f4382a9e58a4696dad76485088bb76` · **levantamento:** 2026-09-25. Este documento separa código existente, estado operacional consultado e propostas. `PROPOSTO` nunca significa alerta vigente.

## Escopo e fontes confirmadas

- API oficial: Spring Boot deste repositório. `src/main/resources/logback-spring.xml` formata o console com aparência JSON, mas usa `PatternLayoutEncoder`; mensagens e exceções não têm escape JSON garantido. Isso é risco conhecido da frente 02, não corrigido aqui.
- Ambiente autorizado consultado somente para leitura: Container App `carsync-api-dev` em estado `Running`; Application Insights `appi-carsync-dev`; workspace real `law-carsync-dev`, retenção 30 dias.
- A tabela operacional comprovada para logs da aplicação é `AppTraces`. Em 30 dias ela continha 23.713 traces, dos quais 24 tinham `SECURITY_VIOLATION`. Não houve ocorrência de `ANALYTICS_ACCESS` ou `DATA_RETENTION` no mesmo recorte.
- `AppMetrics` não retornou `data_retention.removed` em 30 dias. O contador existe no código, mas sua ingestão operacional não está comprovada.
- Não existe `docs/security/sec-2026/02-api/EVENTS.md` nesta base. Consultas podem usar os literais confirmados no código e no ambiente, mas o aceite final do contrato continua dependente da frente 02.
- Mobile: o único app encontrado está sob `test/.../03_mobile/fordretain-app`, é explicitamente exemplo não oficial e não contém SDK/evento de telemetria comprovado.
- IoT: handoff da frente 04, commit `400466ba9f2ae11c624c5feefb0d13a1cf5fd291`, não identificou cliente, broker, firmware, repositório ou owner nominal.
- ML: `test/.../01_ia_ml/src/classificador.py` imprime `classification_report` no treino e grava artefatos locais; não há telemetria operacional, destino ou owner nominal identificados.

## Inventário e plano

| Componente | Sinal | Fonte e formato efetivos | Regra / limiar | Destino | Responsável | Resposta |
|---|---|---|---|---|---|---|
| API | `SECURITY_VIOLATION` — `Auth Failed`, `Account Locked`, `Rate Limit Exceeded`, `JWT Invalid`, `JWT Wrong token type`, `Invalid Token`, `Token Expired` | SLF4J em `RateLimitFilter`, `JwtAuthenticationFilter` e `GlobalExceptionHandler`; `AppTraces.Message` no ambiente | **ATIVO, mas requer correção:** `alert-sec-violations`; consulta filtra bins `>=5/1m` e a condição externa conta linhas `>5`, combinação que não representa simplesmente “5 eventos/min”. Ver `ALERTS.md` | Workspace `law-carsync-dev` → action group `ag-security-email` | Mantenedor backend e responsável Azure da entrega; nomes pessoais não estão versionados | Validar pico e origem, conter credencial/token/cliente abusivo, corrigir causa e confirmar retorno ao baseline |
| API | `ANALYTICS_ACCESS user:{} resource:{}` | SLF4J em cinco endpoints de `AnalyticsController`; destino esperado `AppTraces` | **PROPOSTO, não implantado:** alertar acesso fora do perfil/janela aprovados somente após baseline e política; não usar usuário real em evidência | Application Insights / workspace, sem regra encontrada | Owner de analytics a ser nomeado pelo mantenedor | Confirmar autorização, preservar evidência, revogar sessão/permissão se indevida |
| API | `DATA_RETENTION ...` e contador `data_retention.removed{type=hard_delete|anonymization}` | `DataRetentionService`; log SLF4J e Micrometer | **PROPOSTO, não implantado:** ausência de execução esperada por 2 períodos agendados ou falha do job; valor depende da política e volume. Não alertar por “zero removidos” isolado | Log esperado em `AppTraces`; métrica esperada em `AppMetrics`, ainda não observada | Owner backend/dados a ser confirmado | Verificar scheduler, conectividade e política; não repetir destruição sem idempotência/autoridade |
| API | Login HTTP 2xx/4xx/5xx | Auto-instrumentação em `AppRequests`; não é evento de auditoria de aplicação | **PROPOSTO:** taxa 401/403 >10% em 5 min com ao menos 20 tentativas, após baseline; não vigente | Application Insights, sem regra específica | Owner de autenticação backend | Correlacionar com `SECURITY_VIOLATION`, rate limit e versão; evitar enumeração/PII |
| Mobile | falha de autenticação, erro de rede, crash e falha de storage seguro | **AUSENTE:** exemplo não oficial usa Axios/AsyncStorage e não emite telemetria | **PROPOSTO:** crash-free sessions <99%/24h ou erro de auth/rede >10%/5m com mínimo 20; calibrar no app oficial | **A DEFINIR** no stack oficial; não criar endpoint neste backend | Owner mobile nominal **não identificado**; mantenedor da entrega deve indicar repo/SHA/owner | Invalidar sessão quando aplicável, distinguir 401/403/429/offline, preservar privacidade e corrigir regressão |
| IoT | handshake/cadeia/hostname TLS, auth MQTT, ACL, desconexões e fallback plaintext | **AUSENTE:** nenhum cliente/broker oficial; esquema da frente 04 é apenas proposto | **PROPOSTO:** qualquer fallback plaintext = crítico; >=3 falhas TLS da mesma versão em 5 min = triagem. Recalibrar com baseline real | **A DEFINIR** no broker/cliente reais | Owner IoT nominal **não identificado**; mantenedor integrado deve desbloquear | Bloquear downgrade, validar CA/hostname, revogar identidade comprometida e recuperar sem plaintext |
| ML | falha de treino/inferência, drift de entrada, degradação de qualidade e desatualização do modelo | **PARCIAL LOCAL:** `classification_report` em stdout durante treino; sem execução/telemetria operacional comprovada | **PROPOSTO:** queda de F1 macro >=5 pontos percentuais contra baseline aprovado ou modelo além da janela de atualização definida pelo owner; não vigente | **A DEFINIR** pelo pipeline/serving real | Owner ML nominal **não identificado**; fonte localizada em `01_ia_ml` | Suspender promoção, validar dados/versão, reverter artefato aprovado e retreinar sob mudança controlada |

## Campos, correlação e privacidade

A correlação deve preferir `operation_Id`/timestamp/versão e identificadores pseudonimizados. Evidência não deve registrar JWT, refresh token, senha, secret, CPF, e-mail, VIN bruto, localização, payload IoT, chave/certificado privado ou endereço de destinatário do action group. IP e usuário presentes na mensagem atual devem ser redigidos nas capturas. A frente 02 deve definir o contrato de eventos e corrigir o encoder; 03 deve entregar sinais do cliente oficial; 04 deve entregar sinais do cliente/broker real; o mantenedor deve nomear owner de ML.

## Cobertura dos requisitos

- **R15:** há plano integrado e lacunas explícitas; logs de login/alteração crítica ainda não estão completos como auditoria de aplicação.
- **R16:** API tem fonte operacional e uma regra ativa com problema de semântica; mobile, IoT e ML permanecem sem métrica/alerta operacional comprovado.
- **R17:** nenhuma configuração de dashboard/workbook customizado foi encontrada; captura real continua necessária.
- **R18:** procedimento e revisão de mesa são entregues nos checkpoints T4.
