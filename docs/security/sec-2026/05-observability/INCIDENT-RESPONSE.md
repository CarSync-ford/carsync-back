# Procedimento de resposta a incidentes

**Ambiente conhecido:** Azure Container App `carsync-api-dev`, Application Insights `appi-carsync-dev`, workspace `law-carsync-dev`. **Canal automático comprovado:** action group `ag-security-email`, com um receiver habilitado; endereço omitido. Não existe contato nominal, escala de plantão ou canal de coordenação versionado. Antes de operação, o mantenedor da entrega deve nomear Incident Commander (IC), owners API/Azure/mobile/IoT/ML e o canal do registro do incidente. Esses campos não podem ser preenchidos com pessoas ou salas fictícias.

## Papéis e acionamento

- **Incident Commander:** mantenedor da entrega integrada ou pessoa formalmente delegada; decide severidade, aprova contenção e encerra resposta.
- **Owner Azure/API:** pessoa com RBAC autorizado; executa consultas e, somente após aprovação do IC, mudanças no Container App, identidade ou regra.
- **Owners mobile, IoT e ML:** não identificados nesta base; o mantenedor deve nomeá-los. Até lá, ações nesses componentes são bloqueadas.
- **Owner dados/privacidade:** pessoa indicada pelo mantenedor quando houver possível exposição de dado pessoal, telemetria ou localização.
- **Canal inicial:** alerta pelo `ag-security-email`. **Canal de coordenação:** registro/canal aprovado informado no início do incidente; não há nome comprovado neste repositório.

Severidade inicial: crítica quando houver comprometimento confirmado, exposição de segredo/dado pessoal, fallback IoT plaintext ou impacto amplo; alta para abuso ativo, alteração crítica indevida ou indisponibilidade; moderada para evento isolado contido. O IC ajusta com fatos e impacto, não apenas contagem.

## 1. Detecção

| Campo | Procedimento |
|---|---|
| Dono / canal | Receiver do `ag-security-email` aciona o IC pelo canal aprovado; se a regra não disparar, o maintainer abre registro a partir da revisão manual |
| Ação | Registrar UTC, fonte e intervalo; executar `queries/event-inventory.kql`; consultar `AppRequests` para status/rota sanitizada; conferir estado da regra em `ALERTS.md`; pedir sinais ao owner externo quando mobile/IoT/ML estiver envolvido |
| Cuidado | Regra ativa atual tem condição inconsistente e nenhuma instância/30d; ausência de notificação não descarta o sinal. Não copiar `Message` bruto para o canal |
| Critério de saída | Sinal confirmado ou falso positivo justificado; IC, severidade, componentes/versões, janela UTC e identificador do registro preenchidos |

## 2. Análise

| Campo | Procedimento |
|---|---|
| Dono / canal | IC coordena owners dos componentes no registro aprovado; owner de privacidade entra se houver dado pessoal/telemetria/localização |
| Ação | Construir timeline com `AppTraces`/`AppRequests`, revisão/revision do Container App e deploy; correlacionar por timestamp/operation ID sem publicá-lo; classificar subtipo literal; delimitar contas, rotas, versões e impacto; preservar consultas e hashes das evidências sanitizadas |
| Comandos somente leitura | `az containerapp revision list -g carsync-dev -n carsync-api-dev`; `az containerapp show ...`; `az monitor log-analytics query ...`; `az monitor scheduled-query show ...` |
| Hipóteses específicas | Para a API, considerar credencial/JWT/rate limit; para mobile, sessão/storage/crash; para IoT, TLS/auth/ACL/retry sem downgrade; para ML, dado/modelo/serving e drift. Fontes ausentes são lacuna, não ausência de incidente |
| Critério de saída | Escopo e impacto documentados; hipótese suportada/refutada; indicador de comprometimento e dados afetados listados de forma sanitizada; contenção escolhida com risco e aprovador |

## 3. Contenção

| Campo | Procedimento |
|---|---|
| Dono / canal | IC aprova; owner Azure/API executa; owners externos executam apenas em seus componentes autorizados; decisão fica no registro |
| Ação API/Azure | Opções, não comandos automáticos: reduzir tráfego para revisão afetada/retornar à revisão aprovada, bloquear origem na borda confiável, desabilitar credencial comprometida ou restringir rota. Confirmar primeiro que há revisão saudável e plano de retorno |
| Limites | JWT atual é stateless e não há logout/revogação comprovada; rotação do secret derruba sessões e exige aprovação. Rate limit é por instância. Não alterar regra/dashboard durante preservação sem exportar estado |
| Ação mobile/IoT/ML | Mobile: invalidar sessão/versão afetada sem apagar evidência; IoT: bloquear cliente e revogar identidade no broker real, nunca fazer fallback plaintext; ML: suspender promoção/serving da versão afetada e apontar para artefato aprovado |
| Critério de saída | Propagação/abuso interrompido ou reduzido a risco aceito; serviço essencial preservado; evidência anterior à mudança salva; owner/horário/comando e rollback registrados |

**Ações acima alteram produção e podem causar indisponibilidade ou perda de acesso. São reversíveis apenas se revisão/configuração anterior estiver registrada. Não executar sem autorização explícita do IC e owner do recurso.**

## 4. Erradicação

| Campo | Procedimento |
|---|---|
| Dono / canal | Owner do componente corrige em branch/worktree; IC acompanha no registro; pipeline aprovado é o canal de promoção |
| Ação | Remover causa raiz, rotacionar apenas credenciais comprovadamente afetadas, corrigir configuração/código e adicionar teste de regressão; executar testes e scanners do pipeline; para IoT validar CA/hostname/ACL sem downgrade; para ML validar dados, métricas e artefato |
| Mudança conhecida | A condição de `alert-sec-violations` precisa de correção/teste separado e autorizado; não confundir reparo da detecção com causa do incidente |
| Critério de saída | Causa raiz removida em commit/revisão rastreável; testes e revisão aprovados; credenciais/artefatos comprometidos invalidados; nenhum indicador conhecido permanece no escopo |

## 5. Recuperação

| Campo | Procedimento |
|---|---|
| Dono / canal | IC autoriza; owner Azure/componente restaura gradualmente; status e observação ficam no registro aprovado |
| Ação | Implantar revisão/artefato aprovado pelo pipeline; validar `/actuator/health/liveness` e `/actuator/health/readiness`; executar login/fluxo sintético autorizado sem salvar segredo; conferir erros, latência e sinais de segurança; restaurar tráfego por etapas e manter rollback pronto |
| Componentes externos | Mobile: versão/sessão/storage testados; IoT: conexão TLS positiva e negativas de CA/hostname/plaintext; ML: qualidade, drift e versão do modelo verificadas pelo owner real |
| Critério de saída | Serviço e controles validados, sinais no baseline aprovado por pelo menos duas janelas de avaliação relevantes, sem recorrência conhecida, stakeholders informados e IC registra encerramento/risco residual |

## Evidência, comunicação e pós-incidente

Preservar comandos, UTC, versão, resultado e hash; sanitizar PII/segredos e aplicar acesso mínimo. Não registrar senha/token/chave, mensagem com IP/usuário, payload IoT, VIN/localização ou receiver. Se houver dado pessoal, o owner de privacidade avalia obrigações legais; este documento não substitui aconselhamento jurídico.

Após recuperação, revisar timeline, detecção, atraso, decisões e ações. Abrir tarefas com owner/prazo para: corrigir/testar a regra, publicar `EVENTS.md`, produzir dashboard/captura real e integrar fontes mobile/IoT/ML. Revisão de mesa não é incidente real e deve ser rotulada assim.
