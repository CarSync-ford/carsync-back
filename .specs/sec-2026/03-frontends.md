# 03-frontends — integração de autenticação e criptografia local

Modelo sugerido: **Gemini 3.8 High Effort**. Estado: PENDENTE. Requisitos: R07; insumos R14, R16, R20–R21. Protocolo obrigatório: [EXECUTION.md](EXECUTION.md). Criar worktree `sec-2026/03-frontends` no início do desenvolvimento documental; no frontend, criar worktree próprio a partir do SHA daquele repositório antes de alterar código.

## Base e limites

A verificação explicitamente atribuída ao front/mobile está na spec histórica phase-2: integração após mudança do fluxo de autenticação. Teste Spring não demonstra comportamento do cliente. A rubrica também exige criptografia local e mapeamento Mobile Top 10.

Escrita exclusiva neste repo: `docs/security/sec-2026/03-frontends/**`. Não implementar no backend nem modificar os exemplos sob `test/` sem confirmação de que são o aplicativo entregue. No repo frontend identificado, registrar caminhos exclusivos e responsável no STATUS antes de editar; se web/mobile compartilham arquivos, um único dono ou subfrentes em caminhos disjuntos.

Entrada pronta: [`FRONTEND-HANDOFF.md`](../../docs/security/sec-2026/03-frontends/FRONTEND-HANDOFF.md). Pode iniciar inventário, armazenamento e testes com contrato atual sem aguardar 02; fechamento integrado depende da versão backend corrigida, cujo SHA deve constar da evidência.

## Tasks e checkpoints

| Checkpoint | Entrega curta | Verificação / evidência | Commit atômico |
|---|---|---|---|
| T1.C1 | Identificar fronts realmente entregues, base URL e persistência de dados | Inventário repo/SHA/plataforma/storage/dado; nenhum exemplo tratado como produção | `docs(sec-front): inventory client security boundaries` |
| T2.C1 | Verificar login e refresh pelo contrato real no cliente | UI/rede: campos `email,password`, resposta `token,refreshToken`; erros sem vazamento; expiração/renovação coordenada sem loop; 403/429 não iniciam refresh | `test(sec-front): verify login and refresh integration` |
| T2.C2 | Corrigir incompatibilidades de integração confirmadas | Reexecutar testes C1 no cliente real contra versão identificada da API; teste e correção no mesmo commit | `fix(sec-front): align authentication integration` |
| T3.C1 | Aplicar criptografia local onde há dados sensíveis persistidos | Storage seguro da plataforma, chaves separadas de ciphertext; teste persistência sem plaintext e limpeza de sessão | `fix(sec-front): protect sensitive local storage` |
| T3.C2 | Registrar evidência real de criptografia e limites por plataforma | Trecho, captura sanitizada, commit e explicação; web sem storage sensível não substitui prova de criptografia mobile | `docs(sec-front): evidence local encryption` |
| T4.C1 | Entregar resultados de integração, Mobile Top 10 e sinais mobile disponíveis | Matriz do handoff preenchida; gaps reset/MFA explicitados, nenhum sucesso inventado; encaminhar fontes de sinais a 05 e controles a 06 | `docs(sec-front): hand off verified client security evidence` |

## Aceite e não escopo

Concluir login/refresh/armazenamento no cliente entregue; marcar bloqueio se acesso ausente. Instrução para outro time não prova implementação. Verificar funcionalidades opcionais existentes somente quando expostas no cliente: não criar tela MFA/reset, servidor SMTP ou certificate pinning para cumprir a rubrica. Validação local ajuda UX; validação de entrada, token e autorização continuam obrigatórias no servidor. Uma recomendação de storage não é controle aplicado.

Saídas: handoff atualizado, `REPORT.md`, `STATUS.md` e evidências sanitizadas na pasta exclusiva; código/testes com SHA no repo frontend identificado.
