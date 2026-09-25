# 06-compliance — riscos, continuidade e entrega única

Modelo sugerido: **GPT 6 Astra**. Estado: PENDENTE. Requisitos: R14, R19–R23. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/06-compliance` no início do desenvolvimento.

## Base e limites

Mascaramento/retenção/Envers existem, mas não provam conformidade LGPD completa. `User` auditado pode manter segredos/PII históricos; registrar risco, não afirmar eliminação integral. Documentação Azure não prova backup/restauração executados. Specs antigas fazem alegações não sustentadas e omitem Mobile Top 10.

Escrita exclusiva: `docs/security/sec-2026/06-compliance/**`, `docs/security/sec-2026/ENTREGA.md`. Somente esta frente edita o documento final. Não editar relatórios de outras frentes, código, normas, spec fonte ou documentos históricos; referenciar limitações e evidências atuais. Correção de código necessária identificada aqui é solicitada ao dono 02, não aplicada paralelamente.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Criar esqueleto único da entrega e matriz R01–R23 | Quatro atividades e pesos originais; links/estados das seis frentes, sem check concluído sem evidência | `docs(sec-compliance): structure consolidated security delivery` |
| T2.C1 | Revisar STRIDE associado ao ciclo DevSecOps real | Ameaça/ativo/fronteira/mitigação/evidência/risco residual/dono; API, mobile, IoT, dados, ML e arquitetura considerados | `docs(sec-compliance): review stride and devsecops risks` |
| T2.C2 | Mapear OWASP ASVS e API Top 10 | Declarar edição usada; IDs/títulos conferidos na publicação oficial; aplicado/parcial/pendente/não aplicável com motivo/evidência | `docs(sec-compliance): map asvs and api security practices` |
| T2.C3 | Mapear OWASP Mobile Top 10 com frente 03 | Cobrir categorias da edição escolhida; estado por categoria com evidência ou lacuna; não certificar app sem acesso | `docs(sec-compliance): map mobile security practices` |
| T3.C1 | Mapear LGPD sobre dados pessoais, telemetria e localização | Inventário/finalidade/base legal/responsável/retenção/acesso; distinguir proposta de política aprovada e masking de anonimização | `docs(sec-compliance): map personal and telemetry data handling` |
| T4.C1 | Definir rotinas de dependências, testes e auditoria de permissões | Procedimento/responsável/periodicidade proposta/evidência por rotina; reutilizar CI e matriz 02 | `docs(sec-compliance): define continuous security routines` |
| T4.C2 | Documentar rotina de backup e recuperação existente | Fonte da Fase 3 procurada; local, retenção e acesso verificados; restauração isolada descrita; evidência real se executada, pendência se não | `docs(sec-compliance): document backup and recovery routine` |
| T5.C1 | Incorporar evidências 01–05 no documento único | Trechos, prints, commits reais e explicações por atividade; links relativos resolvem; cada linha R01–R23 tem status honesto | `docs(sec-compliance): consolidate verified security evidence` |
| T5.C2 | Revisar checklist final e eliminar cobranças extras | Confronto integral com SEC-REQUIREMENTS; não conformidades/bloqueios explícitos; nenhuma afirmação de implantação sem prova | `docs(sec-compliance): finalize scoped compliance checklist` |

## Aceite e dependências

T1–T4 iniciam paralelamente; usar contratos publicados e registrar evidências pendentes. T5 exige merge/evidências identificadas das frentes, nunca editar cópia de seus arquivos. Incluir no próprio `ENTREGA.md` conteúdo suficiente das quatro atividades; não entregar apenas um índice de documentos. Anexos podem guardar capturas e logs com links estáveis.

Backup/recuperação: o enunciado pede rotina, não novo serviço, RPO/RTO arbitrário ou exercício destrutivo. Se houver meta já acordada, registrá-la; execução de restore é em ambiente isolado e autorizado, nunca sobre produção. Mapeamento de normas não é certificação ASVS completa nem parecer jurídico LGPD. Dependências externas sem acesso permanecem pendentes, não N/A automáticas.

Saídas: `ENTREGA.md` como documento final; `REPORT.md`, `STATUS.md`, matrizes/rotinas sob `06-compliance/`. Evidências existentes podem ser reaproveitadas com SHA e contexto, sem refazer controles seguros.
