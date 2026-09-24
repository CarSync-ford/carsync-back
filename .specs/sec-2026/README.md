# SEC-2026 — escopo normativo e frentes paralelas

Base auditada: `ceca50ded9c46d1dbc16c2ab1f17217055935e94`, em 2026-09-23. Fonte única de cobrança: [`SEC-REQUIREMENTS.md`](../../SEC-REQUIREMENTS.md). Leitura orientada pelo grafo existente do graphify; confirmar implementação no código atual, não nas arestas do grafo ou em checks históricos. A fase 6 foi revertida na base auditada.

Este pacote substitui as specs `phase-0` a `phase-6` como **backlog desta entrega**, sem desfazer implementações existentes. Não executar ambos os conjuntos cumulativamente. Os arquivos antigos permanecem como histórico.

## Ordem de leitura

1. Este índice e [`EXECUTION.md`](EXECUTION.md).
2. Spec da frente escolhida: cabeçalho, limites de escrita, checkpoints e aceite.
3. Criar o worktree da spec no início de seu desenvolvimento, conforme o protocolo. Não implementar na `main`.

## Specs

| ID | Spec | Modelo sugerido (nomenclatura solicitada) | Dependência de execução |
|---|---|---|---|
| 01-pipeline | [Pipeline e evidências](01-pipeline.md) | OpenAI 5.6 terra | Independente |
| 02-api | [Hardening e perfis](02-api.md) | GPT 6 Astra | Independente; dono dos contratos HTTP existentes |
| 03-frontends | [Criptografia local e validação nos clientes](03-frontends.md) | Gemini 3.8 High Effort | Inicia pelo handoff; validação real depende de acesso aos fronts |
| 04-iot-infra | [MQTT/TLS e infraestrutura](04-iot-infra.md) | OpenAI 5.6 terra | Inicia pela evidência local; IoT/cloud exigem acesso próprio |
| 05-observability | [Monitoramento e resposta](05-observability.md) | OpenAI 5.6 sol | Plano independente; prints reais exigem ambiente |
| 06-compliance | [Riscos e documento final](06-compliance.md) | GPT 6 Astra | Matriz/plano independentes; fechamento após evidências 01–05 |

Os nomes dos modelos são recomendações de alocação fornecidas no pedido, não afirmação de disponibilidade ou benchmark. Nenhuma ferramenta específica é obrigatória quando o enunciado oferece exemplos.

## Matriz de cobertura exata

Linhas abaixo usam a numeração convencional iniciada em 1 do arquivo normativo. Cada item tem um dono; outras frentes entregam insumos, sem editar o artefato do dono.

| ID | Obrigação | Linhas | Dono / aceite verificável |
|---|---|---|---|
| R01 | Diagrama CI/CD focado em segurança | 4–8,17–18 | 01: diagrama e percurso commit–deploy |
| R02 | SAST | 9–11 | 01: etapa, execução/evidência e risco reduzido |
| R03 | SCA | 12–13 | 01: etapa, execução/evidência e risco reduzido |
| R04 | Secret scanning | 14 | 01: etapa, execução/evidência e risco reduzido |
| R05 | Container security, se aplicável | 15 | 01: aplicável, pois há Dockerfile; scan e interpretação |
| R06 | Explicar execução e mitigação de riscos | 16–18 | 01: documento vinculado ao projeto real |
| R07 | Criptografia local | 24–25 | 03: evidência no armazenamento do cliente; infra não substitui |
| R08 | Rate limit | 26–27 | 02: teste do limite e resposta de rejeição |
| R09 | Validação de entrada | 26–27 | 02: rejeição server-side de entrada inválida |
| R10 | JWT seguro | 26–27 | 02: validações e rejeições verificadas |
| R11 | Acesso por perfil: Brigadista, Gestor, Administrador | 28–29 | 02: matriz real, correspondência de domínio explícita e testes |
| R12 | MQTT/TLS para IoT | 30 | 04: configuração cliente/broker, sucesso válido e rejeição TLS inválida |
| R13 | IaC Security, se aplicável | 31–34 | 04: Dockerfile/configuração existente, sem exigir Terraform/Kubernetes |
| R14 | Evidências reais: código, prints, commits, explicações | 24,35–36 | 06: agrega evidências 02–04; ausência não vira conclusão |
| R15 | Logs estruturados: login, falhas, alterações críticas | 42–44 | 05: exemplos sanitizados das três categorias |
| R16 | Plano de métricas e alertas: API, mobile, IoT, ML | 45 | 05: quatro componentes, fonte, regra, destino, resposta |
| R17 | Dashboards e prints | 46–47,51 | 05: painel existente/equivalente e captura real sanitizada |
| R18 | Detecção, análise, contenção, erradicação, recuperação | 48–51 | 05: fluxo e procedimento utilizável |
| R19 | Revisão STRIDE + DevSecOps | 57 | 06: ameaças, mitigação, evidência, risco residual |
| R20 | ASVS, Mobile Top 10, API Top 10 | 58–61 | 06: mapeamento versionado e aplicabilidade, sem alegar certificação |
| R21 | LGPD: dados pessoais, telemetria, localização | 62 | 06: inventário, finalidade/base legal, tratamento e lacunas reais |
| R22 | Rotinas: dependências, testes, permissões, backup/recuperação | 63–67 | 06: responsável, periodicidade proposta, procedimento e evidência |
| R23 | Documento único, quatro atividades + checklist | 1–2,68 | 06: `docs/security/sec-2026/ENTREGA.md`, pesos 3,0 / 2,5 / 2,0 / 2,5 |

## Limites que evitam cobrança extra

- Não exigir SMTP/password reset, MFA, rotação RS256/JWKS, Redis/rate limit distribuído, mTLS em todos os serviços, Key Vault, WAF/CDN específicos, SIEM pago, pentest externo ou metas quantitativas não previstas.
- Não transformar ASVS em certificação integral nem LGPD em construção automática de novas APIs. Mapear realidade, riscos e evidências; corrigir defeitos necessários à obrigação específica.
- Não confundir instrução/documentação de frontend ou IoT com implementação concluída nesses componentes.
- Controles já existentes e seguros devem ser preservados; simplificar o backlog não autoriza desabilitá-los.
- Tokens/segredos/dados pessoais e detalhes sensíveis de infraestrutura não devem ser publicados. Evidências ficam no repositório, sanitizadas; compartilhamento externo depende do responsável.

## Auditoria e entrega ao frontend

- [`AUDIT.md`](AUDIT.md): comparação entre specs históricas, implementação e escopo normativo.
- [`FRONTEND-HANDOFF.md`](../../docs/security/sec-2026/03-frontends/FRONTEND-HANDOFF.md): contrato para pessoa desenvolvedora ou agente de frontend.
- Status inicial de todos os checkpoints: **pendente**. Esta rodada cria planejamento/documentação, não atesta testes, implantação ou conformidade.
