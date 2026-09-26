# Regras de alerta e verificação operacional

Verificação somente leitura realizada em 2026-09-25 no resource group `carsync-dev`. Nenhuma regra, action group ou dashboard foi criado/alterado.

## Regra encontrada

| Item | Estado observado |
|---|---|
| Regra | `alert-sec-violations`, habilitada, severidade 2 |
| Escopo | workspace `law-carsync-dev` |
| Frequência/janela | 1 minuto / 1 minuto |
| Consulta | `AppTraces`, `Message contains "SECURITY_VIOLATION"`, `summarize FailCount=count() by bin(TimeGenerated, 1m)`, filtro `FailCount >= 5` |
| Condição externa | agregação `Count`, operador `GreaterThan`, threshold `5`, 1 de 1 períodos |
| Destino | action group `ag-security-email`, habilitado, um receiver de e-mail habilitado com common alert schema; endereço omitido |
| Ação esperada | notificar o receiver configurado para iniciar triagem do backend/Azure |
| Instância disparada | nenhuma instância dessa regra retornada pela API AlertsManagement no intervalo de 30 dias |

**Ativo não significa disparado nem eficaz.** A regra existe e está habilitada, mas a combinação observada é semanticamente inconsistente com “5 eventos por minuto”: a própria KQL agrega e filtra para no máximo uma linha por bin de um minuto, enquanto a condição externa aplica `Count > 5` sobre as linhas retornadas. No histórico havia um bin com 6 eventos em `2026-09-14T23:28:00Z`, porém nenhuma instância da regra foi encontrada. Não se afirma que uma notificação ocorreu.

### Correção proposta — não aplicada

O responsável Azure deve escolher uma única camada de agregação. Opção a validar em ambiente de teste: retornar eventos brutos `AppTraces` filtrados por `SECURITY_VIOLATION` e aplicar `Count > 4` em janela de 1 minuto; ou retornar uma coluna numérica agregada e configurá-la explicitamente como medida. Antes da implantação: validar a consulta com dados sintéticos autorizados, confirmar common alert schema, testar entrega do action group sem registrar endereço e documentar resolução/fechamento da instância.

## Verificação por sinal pertinente

| Sinal | Fonte verificada | Regra/destino | Ação e estado |
|---|---|---|---|
| `SECURITY_VIOLATION` API | 24 linhas em `AppTraces`/30d; pico de 6 em um bin | regra ativa acima → `ag-security-email` | triagem prevista; eficácia e disparo **não comprovados** devido à condição inconsistente |
| `ANALYTICS_ACCESS` API | emissor existe; 0 linhas/30d | nenhuma regra encontrada | **PLANEJADO:** baseline/política antes de alertar; owner analytics deve validar acesso indevido |
| `DATA_RETENTION` e `data_retention.removed` | emissores existem; 0 traces e 0 métricas/30d | nenhuma regra encontrada | **PLANEJADO:** detectar ausência/falha do job; owner dados deve confirmar scheduler/ingestão |
| Login HTTP | `AppRequests` tem resultados 2xx/4xx/5xx | nenhuma regra específica encontrada | **PLANEJADO:** correlacionar taxa de falha com eventos de segurança e versão |
| Mobile | sem fonte oficial comprovada | nenhuma regra/destino | **BLOQUEADO:** owner mobile fornece app, SDK, redaction e baseline |
| IoT | sem cliente/broker/fonte comprovados | nenhuma regra/destino | **BLOQUEADO:** owner IoT fornece callbacks/log do broker e ambiente |
| ML | somente `classification_report` local | nenhuma regra/destino | **BLOQUEADO:** owner ML define pipeline/serving, baseline e destino |

## Roteiro de revalidação

1. Executar `queries/event-inventory.kql` no workspace e registrar somente contagens sanitizadas.
2. Exportar a definição da regra e conferir consulta, janela, operador, threshold, períodos e scope.
3. Conferir que o action group e receiver estão habilitados sem copiar o endereço.
4. Em ambiente autorizado, gerar eventos sintéticos acima e abaixo do limiar; isso **não foi executado nesta rodada**.
5. Consultar AlertsManagement e comprovar uma instância `Fired`/`Resolved` e a entrega pelo destino; regra habilitada sozinha não basta.
6. Após teste, remover dados sintéticos quando aplicável e registrar owner/decisão. Alterações cloud exigem autorização do mantenedor.
