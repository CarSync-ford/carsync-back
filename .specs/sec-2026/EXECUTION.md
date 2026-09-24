# Protocolo de execução das specs SEC-2026

Fonte normativa: `SEC-REQUIREMENTS.md`. Este protocolo organiza trabalho; não acrescenta requisitos de segurança. Specs antigas são histórico, não backlog cumulativo.

## Preparação obrigatória de cada spec

1. Integrar primeiro este pacote documental na branch-base escolhida pelo mantenedor. Não iniciar implementação a partir de uma base sem estas specs.
2. Registrar o SHA dessa mesma base como `BASE_SHA` para todas as frentes. Verificar `git status --short` e `git worktree list`; não reutilizar branches/worktrees de outras tarefas.
3. No início do desenvolvimento de **cada** spec, criar seu próprio worktree. Na raiz do repositório, substituir `ID` pelo identificador do cabeçalho:

   ```bash
   git worktree add -b "sec-2026/ID" "../carsync-sec-2026-ID" "$BASE_SHA"
   ```

4. Trabalhar apenas nesse worktree e na lista de escrita da spec. Ler qualquer arquivo é permitido; editar arquivos de outra frente, não. Spec externa exige worktree também no repositório frontend/IoT efetivamente identificado, com sua própria base registrada.
5. Publicar contrato e evidências na pasta exclusiva da frente. Não editar README, este protocolo ou documento consolidado durante execução paralela.

A criação dos worktrees acontece no início da implementação, não na redação destas specs. Não tocar nos worktrees preexistentes.

## Checkpoints e commits

- Cada linha `Tn.Cn` é um checkpoint pequeno: um objetivo, uma verificação, um commit atômico. Estimativa de execução: 15–45 minutos; dividir antes de implementar se maior. Estimativa não é critério de nota.
- Teste e correção do mesmo comportamento entram juntos no mesmo commit. Não comitar estado deliberadamente quebrado.
- Mensagem Conventional Commits fornecida pela spec; adaptar `docs` para `fix`/`test` quando o conteúdo exigir, mantendo escopo exclusivo.
- `git add` somente dos caminhos explícitos do checkpoint; nunca `git add .`. Rodar validação e `git diff --cached --check` antes do commit.
- Registrar evidência no mesmo commit: comando, resultado real, ambiente, data e referência ao teste/arquivo. Registrar o SHA depois, na consolidação, para evitar autorreferência impossível do commit.
- Todo commit criado por agente encerra com `Co-Authored-By: Claude Code <noreply@anthropic.com>` quando essa atribuição for aplicável ao executor. Não atribuir trabalho de outro modelo ao Claude; seguir a política do executor nesse caso.
- Cada checkpoint executado deixa um commit atômico, mesmo quando reutiliza controle existente: nesse caso, comitar somente sua verificação documentada em `STATUS.md`, com estado `REUTILIZADO`, SHA da implementação e resultado atual. Não produzir commit vazio nem reimplementar controle para justificar commit.

## Contrato assíncrono de evidências

Cada frente mantém apenas `docs/security/sec-2026/<ID>/STATUS.md`, mais seus entregáveis exclusivos. Formato:

```text
Spec / base SHA / responsável / repositório:
Checkpoint: T1.C1
Estado: PENDENTE | EM_EXECUCAO | VERIFICADO | REUTILIZADO | BLOQUEADO
Requisito: Rxx
Arquivos e teste/comando:
Resultado observado e data:
Evidência: caminho/link e versão; sem dados pessoais ou segredos
Dependência externa / responsável / ação para desbloquear:
```

Código existente não equivale a teste executado. Documento não equivale a configuração implantada. Exemplo não equivale a print real. `BLOQUEADO` nunca conta como concluído. Não inventar URLs, SHAs, execuções, incidentes ou screenshots.

## Paralelismo e conflitos

- As frentes iniciam juntas a partir de `BASE_SHA`. Cada uma escreve em caminhos exclusivos; contratos existentes de API permanecem estáveis.
- Qualquer mudança necessária fora da lista de escrita vira impedimento no `STATUS.md`, com arquivo, motivação normativa e menor alteração proposta. O mantenedor atribui um único dono antes de liberar a mudança.
- Não alterar dependências, configurações globais, migrations, enums de papel ou contratos HTTP incidentalmente. A spec dona desses arquivos decide; demais frentes usam contratos atuais e registram discrepâncias.
- Não criar endpoints de telemetria, serviços, frameworks, Terraform ou infraestrutura só para preencher documentação. Reutilizar o que existe; pendências reais de outro componente ficam atribuídas a ele.
- Integração por PR/merge serializado pelo mantenedor; desenvolvimento continua assíncrono. Rebase/merge da base atualizada somente no próprio worktree, sem force-push não autorizado.
- Consolidação final depende das evidências, não bloqueia a redação inicial das frentes. Se faltar acesso a frontend, IoT ou cloud, registrar bloqueio e entregar instruções executáveis; a entrega acadêmica continua incompleta nesse item.

## Critério de encerramento

Todos os requisitos R01–R23 do README com evidência real ou não aplicabilidade justificada onde o enunciado permite. Nada além do enunciado vira obrigação. Não remover controles existentes para reduzir escopo. Documento final único, separado nas quatro atividades e pesos originais, produzido exclusivamente pela frente `06-compliance`.
