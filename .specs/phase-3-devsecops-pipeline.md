# Phase 3 — Pipeline DevSecOps

**Estado:** plano revisado; verificar workflow atual antes de implementar.
**Origem:** `SEC-REQUIREMENTS.md:4–18` (PIPE).
**Contrato de execução:** `.specs/README.md`, uma tarefa por vez.

## Resultado exigido

Atividade 1 de `docs/security/SEC-DELIVERY.md`: desenho CI/CD do commit ao deploy, etapas SAST/SCA/Secret Scanning/Container Security aplicável, riscos reduzidos e explicação da execução no Ford. O enunciado aceita explicar como o pipeline seria executado; não apresentar configuração planejada como execução comprovada.

Reutilizar GitHub Actions e workflow de deploy existente. Não criar outra plataforma CI, ambiente de produção ou segundo pipeline concorrente. Não adicionar assinatura de imagens, SBOM ou serviço pago como nova obrigação.

## PIPE-1 — Inventário e desenho mínimo [Low]

**Entradas:** `.github/workflows/`, `pom.xml`, `Dockerfile`, `.github/dependabot.yml` se existir.

1. Identificar triggers, build/teste, construção de imagem, push e deploy reais. Não assumir nome de job ou variável.
2. Desenhar Mermaid com: commit/PR, testes, SAST, SCA, secrets, build de imagem, scan de imagem, push e deploy. Representar dependências reais; scans independentes podem executar em paralelo.
3. Separar PR de deploy. PR não recebe credenciais de produção nem publica imagem/deploy; deploy depende dos checks de segurança aplicáveis.
4. Registrar arquivos/componentes Ford cobertos. Dependências de mobile/IoT/ML em outros repositórios ficam identificadas, não presumidas como escaneadas por Maven.

**Saída:** diagrama e tabela `etapa | gatilho | ferramenta | artefato analisado | risco | resultado/planejado`, na atividade 1.
**Aceite:** todos os estágios do enunciado rastreáveis; branches/triggers consistentes com workflow real.

## PIPE-2 — Scans necessários [High]

**Arquivos-alvo:** workflow existente em `.github/workflows/`; `.github/dependabot.yml`; `pom.xml` somente se ferramenta escolhida exigir.

Escolhas mínimas, salvo equivalente já instalado:

- SAST: Semgrep para fontes Java/Spring. Conferir interface suportada e versão atual antes de configurar; não copiar o antigo `returntocorp/semgrep-action@v1` sem validação.
- SCA: reutilizar scanner existente; na ausência, usar Trivy em modo filesystem para dependências suportadas. Se não cobrir dependências Maven resolvidas/transitivas neste projeto, usar OWASP Dependency Check **em substituição**, não acrescentar scanners redundantes. Registrar cobertura e limitações.
- Secret Scanning: Gitleaks, com saída redigida e escopo de histórico documentado.
- Container Security: Trivy na imagem efetivamente produzida pelo Dockerfile, antes da publicação/deploy. Scan de imagem não comprova análise de configuração do Dockerfile; essa revisão pertence à phase-0.
- Revisão contínua de dependências: Dependabot, se ainda ausente, usando chave correta `package-ecosystem: maven` e frequência semanal. Isso apoia CONT sem criar outro serviço.

Passos:

1. Verificar documentação da versão escolhida. Fixar versões/referências imutáveis de ações, não `@master`; não inventar inputs de actions.
2. Usar permissões mínimas. Não usar execução privilegiada de código de PR não confiável com secrets (`pull_request_target` com checkout do PR, por exemplo).
3. Fazer findings altos/críticos de SAST/SCA/container e secrets confirmados falharem o check. Configurar comando/exit code real; texto no README não bloqueia merge.
4. Não ocultar vulnerabilidades sem correção disponível silenciosamente. Exceções precisam justificativa, prazo e responsável na entrega, sem expor segredo.
5. Manter relatórios sanitizados como evidência. SARIF é opcional se integração existente o suportar, não nova entrega.

**Aceite:** configuração válida; comandos compatíveis com versões; caso limpo passa e fixture sintética segura demonstra falha de cada scanner aplicável. Nunca inserir segredo real ou código explorável na aplicação para testar scanner. Se GitHub/registry indisponível, registrar apenas validação local, deixando execução remota pendente.

## PIPE-3 — Evidências e redução de riscos [Low]

**Dependência:** PIPE-1 e PIPE-2 ou identificação explícita do que permanece planejado.

1. Referenciar workflow/comandos e resultados reais, quando executados, na atividade 1.
2. Explicar ao menos: SAST detecta padrões inseguros; SCA detecta dependências vulneráveis; secrets detecta credenciais expostas; container detecta vulnerabilidades na imagem.
3. Relacionar cada etapa a riscos da revisão STRIDE/DevSecOps, sem afirmar que scanner elimina todo risco.
4. Explicar como falha impede avanço até deploy. Branch protection exige configuração no GitHub; não declarar merge bloqueado se só existe job no YAML.

**Aceite:** documento + diagrama + explicação da execução Ford, com estado honesto de cada evidência.

## Checklist

- [ ] PIPE-1: desenho commit–deploy e cobertura dos componentes.
- [ ] PIPE-2: SAST, SCA, secrets e container aplicável preparados/validados, com estado registrado.
- [ ] PIPE-3: riscos e evidências na atividade 1 do documento único.
