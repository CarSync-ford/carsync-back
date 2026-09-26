# Revisão de mesa — pico de eventos de segurança

**Tipo:** exercício documental controlado; **não é incidente real**. **Data:** 2026-09-25. **Facilitador/documentador:** agente Kiro. Nenhum IC, owner operacional ou stakeholder humano participou; nenhuma decisão produtiva foi autorizada. Nenhum evento foi gerado, alerta acionado, tráfego alterado ou credencial usada.

## Cenário

Para tornar o roteiro verificável sem atacar o ambiente, o exercício reutilizou somente uma distribuição histórica sanitizada como entrada fictícia: seis `SECURITY_VIOLATION` entre `2026-09-14T23:28:00Z` e `23:29:00Z`, sendo cinco `JWT Invalid` e um `Rate Limit Exceeded`. Isso não classifica o episódio histórico como incidente. O cenário de mesa assume que a mesma distribuição acabou de ocorrer e pergunta como o procedimento responderia.

Objetivo: validar detecção, análise, contenção, erradicação e recuperação para os componentes realmente disponíveis, além de revelar lacunas de execução.

## Percurso e decisões

| Etapa | Injeção/ação de mesa | Resultado da revisão | Critério de saída |
|---|---|---|---|
| Detecção | A regra deveria sinalizar o pico; comparar configuração com `queries/event-inventory.kql` | **FALHA AUTOMÁTICA IDENTIFICADA:** KQL interna retorna uma linha, mas condição externa exige `Count > 5`; nenhuma instância de alerta/30d. Caminho manual detecta seis eventos | atingível manualmente após nomear IC/severidade/registro; não atingível pelo alerta atual sem correção |
| Análise | Consultar `AppTraces`, revisão do Container App e `AppRequests`; construir timeline sem copiar mensagem bruta | Comandos/fontes existem. Distribuição 5+1 é recuperável; atribuição, IP e impacto exigiriam acesso restrito aos dados originais em incidente real | atingível se IC/owner forem nomeados e escopo/impacto registrados |
| Contenção | Avaliar rollback de revisão, bloqueio na borda e invalidação de credencial; não executar | Procedimento oferece opções e alerta sobre JWT stateless/rate limit por instância. Nenhuma opção foi escolhida/executada sem causa, owner e autorização | decisão é válida somente com aprovador, evidência prévia e rollback; não testado operacionalmente |
| Erradicação | Definir correção da causa e da detecção pelo fluxo normal de branch/teste/pipeline | Fluxo corresponde ao repo. Corrigir regra não substitui investigar JWTs inválidos; frente 02 também deve publicar contrato/eventos | atingível por commits/testes aprovados; não executado neste exercício |
| Recuperação | Validar health probes, login sintético, sinais e retorno gradual | Endpoints/procedimento existem, mas nenhuma recuperação foi executada. Mobile/IoT/ML não podem ser validados sem owners/fontes | exige duas janelas estáveis, testes por componente e encerramento do IC; somente critério revisado |

## Achados da revisão

1. **Ação prioritária — alerta:** owner Azure deve corrigir uma das camadas de agregação e fazer teste sintético autorizado com prova `Fired`/`Resolved` e entrega ao action group.
2. **Contrato:** frente 02 deve entregar `EVENTS.md` e eventos explícitos para sucesso de login e alteração crítica concluída.
3. **Dashboard:** owner Azure deve salvar painel/workbook equivalente e produzir captura real; não há recurso atual.
4. **Governança:** mantenedor deve nomear IC, owners e canal de coordenação. O action group é canal de notificação, não registro completo do incidente.
5. **Componentes externos:** owners mobile/IoT/ML e suas fontes continuam ausentes; o exercício não pode validar contenção/recuperação nesses componentes.
6. **Privacidade:** consulta operacional pode exigir IP/usuário, mas compartilhamento/evidência deve manter acesso mínimo e redaction.

## Veredito

O procedimento é coerente com Azure Container Apps/Application Insights e diferencia comandos somente leitura de ações produtivas. A revisão de mesa foi concluída, porém não demonstra prontidão operacional: a detecção automática falharia no cenário, owners/canal não estão nomeados e etapas mutáveis não foram ensaiadas. Esses resultados permanecem visíveis; nenhum deles é promovido a incidente ou teste de produção.
