# Status — 05-observability

- **Spec / base SHA / responsável / repositório:** `.specs/sec-2026/05-observability.md` / `41d2227eb7f4382a9e58a4696dad76485088bb76` / agente Kiro sob revisão do mantenedor / `git@github.com:CarSync-ford/carsync-back.git`
- **Worktree:** `/home/enzo/College/carsync-sec-2026-05-observability`, branch `sec-2026/05-observability`
- **Data:** 2026-09-25

## T1.C1 — inventário e plano multicomponente

- **Estado:** VERIFICADO (inventário/plano; não equivale a telemetria ausente)
- **Requisito:** R15–R16
- **Arquivos e teste/comando:** `MONITORING.md`; Graphify sobre o grafo de 1.476 nós; inspeção de fontes; consultas Azure somente leitura registradas em `evidence/T1-C1-research.txt`.
- **Resultado observado e data:** em 2026-09-25 foram mapeados API, mobile, IoT e ML com sinal, fonte, regra, destino, responsável e resposta. Valores propostos estão marcados como não vigentes. API possui `AppTraces`; demais componentes têm lacunas explícitas. `EVENTS.md` da frente 02 não existe nesta base.
- **Evidência:** `MONITORING.md` e `evidence/T1-C1-research.txt`; sem PII ou segredos.
- **Dependência externa / responsável / ação para desbloquear:** frente 02 deve publicar `EVENTS.md`; owner mobile/IoT/ML deve ser nomeado e fornecer fontes reais. Mantenedor da entrega integrada coordena a atribuição.

## T2.C1 — consultas sobre eventos efetivamente emitidos

- **Estado:** BLOQUEADO (consultas executadas; contrato `EVENTS.md` ausente)
- **Requisito:** R15–R16
- **Arquivos e teste/comando:** `queries/event-inventory.kql`, `queries/security-events.kql`, `queries/README.md`; execução com `az monitor log-analytics query` contra `AppTraces`; resultado em `evidence/T2-C1-query-results.json`.
- **Resultado observado e data:** em 2026-09-25 as consultas executaram na tabela real e retornaram 24 `SECURITY_VIOLATION` em 30 dias: 5 `Auth Failed`, 16 `JWT Invalid`, 2 `Rate Limit Exceeded` e 1 `Invalid Token`. `ANALYTICS_ACCESS` e `DATA_RETENTION` tiveram zero ocorrências. Rótulos derivados não são apresentados como campos emitidos.
- **Evidência:** JSON parseável e sanitizado; `Message`, IP, usuário, IDs de assinatura e endereço do destinatário não foram retidos.
- **Dependência externa / responsável / ação para desbloquear:** frente 02 deve publicar `docs/security/sec-2026/02-api/EVENTS.md`. Depois da integração, a frente 05 compara nomes/campos/redaction e repete as consultas na versão implantada.
