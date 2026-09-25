# 01-pipeline — pipeline DevSecOps

Modelo sugerido: **OpenAI 5.6 terra**. Estado: PENDENTE. Requisitos: R01–R06. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/01-pipeline` no início do desenvolvimento.

## Base e limites

`.github/workflows/deploy.yml` executa testes/build/push/deploy em push na main; scanners não existem nesta base. Reutilizar esse fluxo, sem exigir migração de provedor.

Escrita exclusiva: `.github/workflows/**`, `.github/dependabot.yml`, configurações de scanners novas na raiz com nomes registrados no STATUS, `pom.xml`, `docs/security/sec-2026/01-pipeline/**`. Dockerfile pertence à 04; Java pertence à 02. Mudança no pom solicitada por outra frente passa por este dono e deve ser justificada; preferir ferramentas já disponíveis.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Desenhar fluxo atual/proposto commit, PR, testes, SAST/SCA/secrets, imagem, container scan, deploy | Diagrama Mermaid + tabela etapa/entrada/risco/resultado; não declarar execução que não ocorreu | `docs(sec-pipeline): map security stages and risks` |
| T2.C1 | Configurar SAST no CI, usando uma ferramenta adequada | YAML válido; execução local validada; resultado interpretado e sanitizado; execução remota registrada como evidência adicional, não bloqueante | `ci(sec-pipeline): add static security analysis` |
| T2.C2 | Configurar SCA e revisão de dependências sem duplicar scanners | Relatório real local; distinguir atualização de dependência de análise de vulnerabilidade | `ci(sec-pipeline): add dependency security analysis` |
| T2.C3 | Configurar secret scanning, sem incluir segredo real em teste | Execução e teste local seguro com fixture sintética; não comitar credencial | `ci(sec-pipeline): add secret scanning` |
| T2.C4 | Analisar imagem construída pelo Dockerfile existente | Scan da mesma imagem/artefato destinado ao deploy; **repetir se Dockerfile mudar**; resultado, digest e limitações registrados | `ci(sec-pipeline): scan deployment container` |
| T3.C1 | Integrar resultados ao fluxo e documentar como executar | Demonstrar tratamento de falha, permissões mínimas e deploy não disparado por PR não confiável; link de run se disponível | `docs(sec-pipeline): record integrated pipeline evidence` |
| T3.C2 | Re-scan da imagem se Dockerfile mudar durante a consolidação | Executar T2.C4 novamente sobre digest final; registrar alteração e resultado no REPORT | `ci(sec-pipeline): re-scan final deployment container` |

## Aceite e não escopo

Explicar cada etapa e o risco reduzido; apresentar diagrama e execução no projeto Ford. Separar evidência local de CI remoto e de deploy real. Sem acesso ao Actions, marcar verificação remota bloqueada, não simular run. Aplicação de limites/gates é decisão técnica documentada, não meta inventada pelo enunciado. Não exigir OIDC, SBOM, assinatura de imagem, DAST, nova infraestrutura ou ferramenta paga. Não alterar segredos/deploy cloud nesta tarefa sem autorização específica.

Saída para consolidação: `docs/security/sec-2026/01-pipeline/REPORT.md` e `STATUS.md`, links e evidências sanitizadas na mesma pasta.
