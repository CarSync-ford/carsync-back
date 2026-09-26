# Evidência estruturada de login, falha e alteração crítica

Consulta somente leitura executada em 2026-09-25 no workspace `law-carsync-dev`, intervalo de 30 dias. `queries/audit-samples.kql` seleciona linhas individuais e não projeta `Message`, URL completa, usuário, IP ou correlação. Os arquivos em `captures/audit/` são JSON parseáveis, produzidos como projeções sanitizadas dessas linhas reais.

| Caso | Fonte real | Resultado capturado | Limite |
|---|---|---|---|
| login bem-sucedido | `AppRequests` | `POST /api/v1/auth`, HTTP 200 em `2026-09-21T02:09:49.088Z` | telemetria HTTP automática; **não** é evento explícito de auditoria da aplicação e a ação não foi produzida nesta sessão |
| falha de login | `AppTraces` | literal emitido `SECURITY_VIOLATION Auth Failed` em `2026-09-21T01:25:33.461Z` | mensagem bruta/IP omitidos; evento de aplicação confirmado |
| alteração crítica | `AppRequests` | tentativa `MFA_CHANGE`, HTTP 500 em `2026-09-21T01:27:03.171Z` | a mudança **não foi concluída** e não existe evento explícito de auditoria da alteração |

A consulta histórica também encontrou 25 logins HTTP 200, 5 logins 401 e tentativas de MFA/password change/reset apenas com 400/500. Não foram usadas credenciais nem executada escrita no ambiente cloud. Assim, há amostras estruturadas reais para login HTTP e falha, e uma tentativa real de mudança crítica, mas **não** há prova de alteração crítica concluída nem contrato completo de eventos.

## Validação e redaction

- `python3 -m json.tool captures/audit/*.json` valida sintaxe.
- Timestamps, tabela, ação categorizada, resultado e flags de limite foram preservados.
- Foram removidos: mensagem bruta, IP, usuário/e-mail, token, senha, URL completa, operation ID, subscription ID e endereço do receiver.
- O encoder em `logback-spring.xml` continua sem garantia de escape JSON. Estes arquivos são projeções Azure válidas, não prova de que cada linha de console seja JSON válido.
- `AUTH_FAILED`, `RATE_LIMIT_EXCEEDED` e `IP_s` não são apresentados como nomes/campos emitidos.

## Bloqueio restante

T3.C2 permanece `BLOQUEADO` até a frente 02 publicar `EVENTS.md`, emitir evento explícito de sucesso de login e evento de alteração crítica concluída com schema/redaction, e até ações sintéticas controladas serem executadas em ambiente autorizado. A menor correção é responsabilidade da frente 02; esta frente não altera `src/**`.
