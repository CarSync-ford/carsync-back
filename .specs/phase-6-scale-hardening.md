# Phase 6 — Antigo plano de escala cancelado

**Estado:** não executar. Substituído por escopo rastreado em `.specs/README.md`.

`SEC-REQUIREMENTS.md` exige hardening de API e rotinas contínuas, não expansão de infraestrutura. O plano anterior criava obrigações adicionais:

| Plano anterior | Decisão |
|---|---|
| Redis/Bucket4j distribuído | Retirado. Manter rate limit existente e documentar limite por instância em phase-2. Se topologia real exigir correção para garantir controle demonstrado, registrar decisão específica antes de mudar arquitetura. |
| Provisionar APIM e migrar autenticação/rate limit para borda | Retirado. Não instalar gateway nem trocar emissor JWT. |
| Criar frente Cloudflare/WAF/CDN | Retirada. TLS seguro já existente deve ser preservado; não desativar Full (Strict) nem reduzir segurança. |
| Meta SSL Labs A+, assinatura de imagens/SBOM, programa de pentest/drills | Não são entregas obrigatórias deste escopo. Não criar tarefas novas para esses itens. |
| Calendário próprio de segurança contínua | Consolidado em phase-5, apenas nas quatro rotinas exigidas. |

## Limites da retirada

- Cancelamento do plano não autoriza desprovisionar infraestrutura, encerrar contratos, apagar dados ou remover controles já necessários ao ambiente.
- Não desabilitar `RateLimitFilter` por expectativa de APIM futuro.
- Se componente de escala já existir, registrar uso real e custo/dependências; sua desativação exige decisão separada. Não afirmar remoção de recurso externo sem executar e verificar.
- Excedentes de aplicação já identificados (recuperação de senha, MFA, ANALYST e automações arbitrárias de retenção) têm plano de retirada em `phase-7-scope-cleanup.md`.

Nenhuma tarefa de implementação permanece nesta fase.
