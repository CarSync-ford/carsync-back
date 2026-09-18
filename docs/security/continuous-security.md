# Rotinas contínuas de segurança

## Escopo e evidências

Calendário operacional da Fase 5, definido em **2026-09-18**. Horários em **UTC**; responsáveis abaixo são funções, não pessoas já designadas. Security e Infra devem nomear responsáveis e suplentes antes da primeira execução. Este documento não cria eventos em calendários externos nem comprova execução das atividades.

Baseline do repositório: `5740790d57d2fe609fe05ceea3342f13bc5dbfb8`. Ver [STRIDE](threat-model.md), [ASVS 4.0.3](asvs-checklist.md), [API Top 10 2023](api-top10.md) e [backup/recovery](backup-recovery.md). Controles descritos em código não substituem evidências do ambiente implantado.

## Calendário recorrente

| Frequência | Agenda UTC | Próxima execução | Atividade | Responsável / aprovação | Evidência de conclusão |
|---|---|---|---|---|---|
| Semanal | Segunda-feira, 14:00 | 2026-09-21 | Revisar PRs Dependabot de Maven e GitHub Actions; avaliar changelog, compatibilidade, testes e scans antes de merge | Dev Team / revisor distinto do autor | PR, decisão, commit e links dos jobs; registrar adiamentos |
| Mensal | Dia 1, 14:00 | 2026-10-01 | Revisar relatório OWASP Dependency-Check, achados Semgrep/Gitleaks/Trivy e supressões | Security / Dev Team | Execução recente, relatórios, issues com responsável e prazo |
| Trimestral | Dias 15 de janeiro, abril, julho e outubro, 14:00 | 2026-10-15 | Auditar roles da aplicação, acessos GitHub/ACR, Azure RBAC e acesso a ACA Secrets; revisar necessidade e rotação de credenciais | Security + Infra / responsáveis pelos recursos | Matriz de permissões sanitizada, revogações e aprovações |
| Trimestral | Dias 15 de janeiro, abril, julho e outubro, 16:00 | 2026-10-15 | Executar PITR isolado e validação de integridade conforme runbook | Infra / Security + Dev Team | Registro do exercício, contagens, FKs, Envers, RPO/RTO medidos e aceite |
| Semestral | Dias 15 de maio e novembro, 14:00 | 2026-11-15 | Pentest interno ou contratado, com autorização escrita, escopo e janela definidos | Security / responsável pelo sistema | Autorização, relatório restrito, issues e reteste |
| Semestral | Dias 15 de junho e dezembro, 14:00 | 2026-12-15 | Revisão de configuração TLS, ciphersuites e validação SSL Labs A+ | Cloud Infra / Security | Relatório SSL Labs A+, verificação de renovação de certificados |
| Anual | Dia 1 de outubro, 14:00 | 2026-10-01 | Rotação de credenciais críticas (segredo JWT, chave HMAC, senhas do banco de dados) | Security + Dev Team | Registro de rotação sem downtime e validação de revogação antiga |
| Anual | Dia 1 de dezembro, 14:00 | 2026-12-01 | Revisar STRIDE, revalidar ASVS e executar simulado de resposta a incidentes | Security + todas as equipes / responsável pelo sistema | Diff dos documentos, evidências por requisito e ata do simulado |

Se a data cair em dia não útil, antecipar para o último dia útil sem alterar a recorrência. Criar ticket por ocorrência; responsável registra conclusão ou impedimento até o próximo dia útil. Atividades vencidas devem ser escaladas ao responsável pelo sistema, nunca encerradas sem evidência. Datas são agenda definida; execução e aceite permanecem pendentes.

Dependabot já possui agendamento semanal em [`.github/dependabot.yml`](../../.github/dependabot.yml). A revisão humana acima é independente do horário de abertura automática dos PRs. Demais rotinas são manuais; não existe workflow periódico comprovando sua execução. Lembretes de sessão não substituem calendário durável da equipe.

## Critérios operacionais

### Revisões semanais e mensais

1. Registrar commit, ambiente, data UTC e URL de cada execução analisada. Se não houver relatório recente, solicitar execução aprovada do pipeline; ausência de relatório não significa ausência de vulnerabilidades.
2. Revisar achados e falhas dos scanners. Scan que não executou, ficou indisponível ou terminou sem relatório é pendência, não aprovação. Confirmar os gates reais em [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) e [`pom.xml`](../../pom.xml).
3. Criar issue para cada achado válido, com impacto, severidade, responsável, prazo e reteste. Priorizar exposição de credenciais e exploração ativa; nesses casos acionar resposta a incidentes imediatamente, sem esperar a rotina.
4. Supressões exigem justificativa verificável, escopo mínimo, revisor e data de reavaliação. Revisar [`dependency-check-suppressions.xml`](../../dependency-check-suppressions.xml); não reduzir gates para encerrar PR.
5. Merge exige revisão e resultados adequados ao risco. Verificar separadamente regras de proteção de branch e checks obrigatórios no GitHub; YAML não comprova essas configurações.

### Auditoria trimestral

- Comparar acessos efetivos com necessidade de negócio; remover acessos órfãos mediante aprovação.
- Verificar roles `USER`, `ANALYST` e `ADMIN`, contas de serviço e separação entre migração e execução no banco.
- Revisar escopo de credenciais de deploy e registry, validade e possibilidade de migração para OIDC; não registrar valores de secrets em tickets.
- Inspecionar TLS cliente/ingress/banco, configuração Cloudflare/ACA, retenção de logs/backups e exposição de Swagger/Actuator no ambiente real.
- Registrar configurações ausentes ou não verificáveis como pendências, com owner e prazo.
- Executar [backup/recovery](backup-recovery.md); reconciliar anonimizações e eliminações posteriores ao ponto restaurado antes de qualquer retorno a produção.

### Pentest e revisão anual

Pentest deve cobrir autorização por objeto e função, exposição de propriedades, autenticação, replay, limitação de recursos e fluxos sensíveis. Usar dados sintéticos e ambiente autorizado; testes destrutivos ou de carga precisam de autorização específica.

Simulado anual: detecção de credencial comprometida ou exposição de PII, triagem, contenção, revogação/rotação, avaliação de impacto, recuperação e lições aprendidas. Security aciona responsável por privacidade/jurídico para avaliar obrigações LGPD e prazos legais. Não enviar notificações externas sem aprovação dos responsáveis.

Além da revisão anual, atualizar STRIDE/ASVS/API Top 10 quando houver nova fronteira de confiança, integração externa, alteração de autenticação, novo fluxo de dados pessoais ou incidente. Exceções e riscos aceitos exigem responsável, prazo e justificativa; aceitação não transforma controle ausente em implementado.

## Registro de execução

Copiar para ticket de acesso restrito a cada ocorrência:

```text
Atividade / competência:
Data planejada / data executada (UTC):
Responsável / revisor:
Ambiente / commit / imagem:
Escopo e autorização:
Links para PRs, jobs e evidências sanitizadas:
Resultados / pendências / riscos aceitos com validade:
Ações corretivas, responsáveis e prazos:
Resultado: aprovado | reprovado | bloqueado
Próxima execução:
```

Guardar relatórios em repositório de evidências com acesso restrito e retenção aprovada por Security/privacidade. Não versionar dumps, tokens, senhas, PII ou screenshots não sanitizados. Retenção de artefatos CI não equivale à retenção formal das evidências: registrar sua localização durável antes de expirarem.

## Estado inicial

- Calendário e procedimento: documentados.
- Designação nominal de responsáveis e convites externos: pendentes.
- Evidências de execução das rotinas, pentest e simulado: não coletadas nesta entrega.
- Primeiro exercício PITR: pendente; não marcar Fase 5 concluída antes do aceite registrado.
