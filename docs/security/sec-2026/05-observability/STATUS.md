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
