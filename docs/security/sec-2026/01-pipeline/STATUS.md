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
Estado: BLOQUEADO
Requisito: R02
Arquivos e teste/comando: `.github/workflows/deploy.yml`; `docker info --format '{{.ServerVersion}}'`; validação estrutural do YAML.
Resultado observado e data: job Semgrep configurado com regras `p/ci`, `p/java-spring` e `p/secrets`, SARIF e gate anterior ao deploy. Execução local bloqueada: `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`; CLI Semgrep ausente; 2026-09-24.
Evidência: `.github/workflows/deploy.yml`; Semgrep CLI `1.178.0` fixada por tag; wrapper `semgrep-action` depreciado removido; nenhuma execução remota declarada.
Dependência externa / responsável / ação para desbloquear: ambiente com Semgrep ou Docker acessível / mantenedor do ambiente / executar o container configurado no workflow ou validar pelo GitHub Actions.

## T2.C2

Checkpoint: T2.C2
Estado: BLOQUEADO
Requisito: R03
Arquivos e teste/comando: `pom.xml`, `.github/dependabot.yml`, `.github/workflows/deploy.yml`, `dependency-check-suppressions.xml`; tentativa local anterior com `9.0.0`; `mvn -B -DskipTests validate` após atualização.
Resultado observado e data: tentativa com `9.0.0` não produziu análise porque NVD respondeu HTTP 403. Plugin atualizado para `13.0.0`, configuração Maven validada e segredo `NVD_API_KEY` confirmado no repositório sem leitura do valor; scan remoto ainda não executado; 2026-09-24. Dependabot atualiza versões; Dependency-Check analisa vulnerabilidades e falha em CVSS >= 7.
Evidência: `dependency-check-suppressions.xml` sem supressões; plugin OWASP `13.0.0`; `mvn validate` com `BUILD SUCCESS`; segredo `NVD_API_KEY` listado pelo GitHub CLI.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / publicar branch e executar o job SCA com o segredo configurado.

## T2.C3

Checkpoint: T2.C3
Estado: VERIFICADO
Requisito: R04
Arquivos e teste/comando: `.github/workflows/deploy.yml`, `.gitleaks.toml`, `.gitleaksignore`; Gitleaks `8.30.1`: `gitleaks detect --source . --config .gitleaks.toml --gitleaks-ignore-path .gitleaksignore --redact` e teste separado com token sintético temporário.
Resultado observado e data: 151 commits e aproximadamente 9,58 MB analisados, zero vazamentos e exit code 0. Allowlist limitada ao cache gerado `graphify-out/cache/`; cinco fingerprints revisados cobrem vetor público RFC 6238 e chaves JWT de teste/exemplo. Fixture sintética nova gerou 1 achado e exit code 1; fixture removida; 2026-09-24.
Evidência: resumo sanitizado neste STATUS; Gitleaks CLI `8.30.1` em container, sem licença de Action; `fetch-depth: 0`; configurações raiz registradas `.gitleaks.toml` e `.gitleaksignore`.
Dependência externa / responsável / ação para desbloquear: nenhuma para execução local; CI remoto ainda depende de publicação da branch.

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
Resultado observado e data: fluxo integrado documentado; 212 testes passaram, zero falhas. Permissões mínimas configuradas e deploy limitado a `push` em `main`. Evidência remota e deploy real não executados porque a branch ainda não foi publicada; 2026-09-24.
Evidência: `docs/security/sec-2026/01-pipeline/REPORT.md`; commits dos checkpoints; nenhuma URL de run inventada.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor do repositório / publicar branch, abrir PR e anexar URL do run; NVD/Docker seguem bloqueios descritos em T2.C2 e T2.C4.

## T3.C2

Checkpoint: T3.C2
Estado: REUTILIZADO
Requisito: R05
Arquivos e teste/comando: `git diff 52565edf5d81beb593c1cb78f8d23e49820cd076 -- Dockerfile`.
Resultado observado e data: nenhum diff no Dockerfile entre a base e o estado final desta frente; re-scan não aplicável antes da consolidação. A verificação T2.C4 permanece bloqueada e deve ser repetida se outra frente alterar o Dockerfile; 2026-09-24.
Evidência: comparação Git sem saída; base `52565edf5d81beb593c1cb78f8d23e49820cd076`.
Dependência externa / responsável / ação para desbloquear: consolidador / repetir build e Trivy caso Dockerfile mude durante integração.
