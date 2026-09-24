# Status — 01-pipeline

Spec / base SHA / responsável / repositório: `01-pipeline` / `52565edf5d81beb593c1cb78f8d23e49820cd076` / Claude Code / `CarSync-ford/carsync-back`

## T1.C1

Checkpoint: T1.C1
Estado: VERIFICADO
Requisito: R01, R06
Arquivos e teste/comando: `docs/security/sec-2026/01-pipeline/REPORT.md`; revisão do Mermaid e da tabela etapa/entrada/risco/resultado.
Resultado observado e data: fluxo inicial e fluxo proposto documentados sem declarar scanners ou deploy como executados; 2026-09-24.
Evidência: `docs/security/sec-2026/01-pipeline/REPORT.md`; base `52565edf5d81beb593c1cb78f8d23e49820cd076`.
Dependência externa / responsável / ação para desbloquear: nenhuma.

## T2.C1

Checkpoint: T2.C1
Estado: VERIFICADO
Requisito: R02
Arquivos e teste/comando: `.github/workflows/deploy.yml`; `gh pr checks 33`.
Resultado observado e data: SAST (Semgrep) aprovado em 29s no commit `6947874`; regras `p/ci`, `p/java` e `p/secrets`; 2026-09-24. Atualização posterior do pom ainda não publicada para nova execução.
Evidência: https://github.com/CarSync-ford/carsync-back/actions/runs/36020010271/job/107702595477
Dependência externa / responsável / ação para desbloquear: nenhuma para execução registrada; repetir CI após publicação da atualização.

## T2.C2

Checkpoint: T2.C2
Estado: BLOQUEADO
Requisito: R03
Arquivos e teste/comando: `pom.xml`; `mvn -B clean verify -Dspring.profiles.active=test`; `mvn -B dependency:tree`; `mvn -B org.owasp:dependency-check-maven:check -DnvdApiMaxRetryCount=1 -DnvdApiDelay=6000`.
Resultado observado e data: SCA remoto com dependências antigas reprovou após 1h58m28s; relatório indicou 12 dependências vulneráveis e 163 vulnerabilidades reportadas. Parent atualizado para Boot 3.5.16, Springdoc para 2.8.17 e PostgreSQL do plugin Flyway alinhado ao BOM. Overrides Commons Lang 3.20.0 e Log4j 2.25.5 corrigem faixas afetadas ainda mantidas pelo BOM. Build local passou com 212 testes, zero falhas/erros/skips, incluindo OpenAPI. Novo SCA local bloqueado por `Invalid API Key, length of 0 too short to provided a masked partial key` e `NoDataException: No documents exist`; `NVD_API_KEY` ausente localmente. Nenhum scan limpo declarado; 2026-09-24.
Evidência: https://github.com/CarSync-ford/carsync-back/actions/runs/36020010271/job/107702824996 ; resumo de versões e validações em `REPORT.md`; supressões vazias e gate CVSS >= 7 preservados.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor / publicar atualização após autorização e executar SCA com segredo NVD configurado; anexar novo relatório antes de liberar merge.

## T2.C3

Checkpoint: T2.C3
Estado: VERIFICADO
Requisito: R04
Arquivos e teste/comando: `.github/workflows/deploy.yml`, `.gitleaks.toml`, `.gitleaksignore`; Gitleaks `8.30.1`: `gitleaks detect --source . --config .gitleaks.toml --gitleaks-ignore-path .gitleaksignore --redact` e teste separado com token sintético temporário.
Resultado observado e data: 151 commits e aproximadamente 9,58 MB analisados, zero vazamentos e exit code 0. Allowlist limitada ao cache gerado `graphify-out/cache/`; cinco fingerprints revisados cobrem vetor público RFC 6238 e chaves JWT de teste/exemplo. Fixture sintética nova gerou 1 achado e exit code 1; fixture removida; 2026-09-24.
Evidência: resumo sanitizado neste STATUS; Gitleaks CLI `8.30.1` em container, sem licença de Action; `fetch-depth: 0`; configurações raiz registradas `.gitleaks.toml` e `.gitleaksignore`.
Dependência externa / responsável / ação para desbloquear: nenhuma para execução local; Gitleaks remoto foi ignorado por depender do SCA reprovado no run `36020010271`.

## T2.C4

Checkpoint: T2.C4
Estado: BLOQUEADO
Requisito: R05
Arquivos e teste/comando: `.github/workflows/deploy.yml`; `docker build -t carsync-api:sec-2026 .`; validação estrutural da ordem dos steps.
Resultado observado e data: ordem `Build Docker image < Scan deployment image < Push approved Docker image < Deploy to Azure Container Apps` validada. Build local bloqueado por `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`; sem imagem, digest ou scan local legítimo; 2026-09-24.
Evidência: Trivy action `ed142fd0673e97e23eac54620cfb913e5ce36c25`, severidades HIGH/CRITICAL, exit code 1, executada sobre a tag `${{ github.sha }}` antes do push.
Dependência externa / responsável / ação para desbloquear: Docker daemon / mantenedor do ambiente / liberar socket, repetir build, registrar ID/digest e executar Trivy sobre `carsync-api:sec-2026`.

## T3.C1

Checkpoint: T3.C1
Estado: BLOQUEADO
Requisito: R01–R06
Arquivos e teste/comando: `.github/workflows/deploy.yml`, `docs/security/sec-2026/01-pipeline/REPORT.md`; `mvn clean test -Dspring.profiles.active=test`; validação YAML e inspeção da ordem dos gates.
Resultado observado e data: fluxo integrado documentado; 212 testes passaram, zero falhas. Permissões mínimas configuradas e deploy limitado a `push` em `main`. Run remoto `36020010271` aprovou testes e SAST, mas reprovou SCA; Gitleaks e deploy não executados. PR #33 aberto; 2026-09-24.
Evidência: `docs/security/sec-2026/01-pipeline/REPORT.md`; commits dos checkpoints; nenhuma URL de run inventada.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor do repositório / publicar correção das dependências após autorização e repetir CI no PR #33; NVD local/Docker seguem bloqueios descritos em T2.C2 e T2.C4.

## T3.C2

Checkpoint: T3.C2
Estado: REUTILIZADO
Requisito: R05
Arquivos e teste/comando: `git diff 52565edf5d81beb593c1cb78f8d23e49820cd076 -- Dockerfile`.
Resultado observado e data: nenhum diff no Dockerfile entre a base e o estado final desta frente; re-scan não aplicável antes da consolidação. A verificação T2.C4 permanece bloqueada e deve ser repetida se outra frente alterar o Dockerfile; 2026-09-24.
Evidência: comparação Git sem saída; base `52565edf5d81beb593c1cb78f8d23e49820cd076`.
Dependência externa / responsável / ação para desbloquear: consolidador / repetir build e Trivy caso Dockerfile mude durante integração.
