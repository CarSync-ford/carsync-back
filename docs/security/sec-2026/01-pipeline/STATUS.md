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
Arquivos e teste/comando: `pom.xml`; `src/main/java/br/com/sprint1/challenge/config/SecurityConfig.java`; `mvn -B clean test -Dspring.profiles.active=test`; `mvn -B dependency:tree`.
Resultado observado e data: SCA remoto reprovou nos runs 36020010271 e 36046716450 (commit c217e15; 8 arquivos vulneráveis, 58 CVEs únicos). Patches Spring 6.x Enterprise-only motivaram migração para Spring Boot 4.1.1 (Framework 7.0.9, Security 7.1.1, Data JPA 4.1.1, Jackson 3.1.5, PostgreSQL JDBC 42.7.13, Flyway 12.4.0, Hibernate 7.4.5.Final, Tomcat 11.0.26 override e Swagger UI WebJar 5.32.15 com DOMPurify 3.4.13 empacotado). Build local passou com 212 testes, zero falhas/erros/skips (38.5s), incluindo OpenAPI, Flyway no H2 e redirecionamento HTTPS. Novo SCA local segue bloqueado por falta de `NVD_API_KEY` no ambiente local; nenhum scan limpo declarado antes de validação no GitHub Actions; 2026-09-24.
Evidência: runs 36020010271 e 36046716450; matriz de versões e árvore Maven documentadas em `REPORT.md`; supressões vazias e gate CVSS >= 7 preservados.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor / publicar commit de migração Boot 4 após autorização e executar CI no PR #33 com segredo NVD; inspecionar novo relatório de SCA.

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
Resultado observado e data: fluxo integrado documentado; 212 testes passaram no Spring Boot 4.1.1, zero falhas. Permissões mínimas configuradas e deploy limitado a `push` em `main`. Run remoto 36046716450 aprovou testes (1m11s) e SAST (33s), mas reprovou SCA com 58 CVEs que demandaram a migração de plataforma. PR #33 aberto; 2026-09-24.
Evidência: `docs/security/sec-2026/01-pipeline/REPORT.md`; commits dos checkpoints; nenhuma URL de run inventada.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor do repositório / publicar migração Boot 4 após autorização e repetir CI no PR #33; NVD local/Docker seguem bloqueios descritos em T2.C2 e T2.C4.

## T3.C2

Checkpoint: T3.C2
Estado: REUTILIZADO
Requisito: R05
Arquivos e teste/comando: `git diff 52565edf5d81beb593c1cb78f8d23e49820cd076 -- Dockerfile`.
Resultado observado e data: nenhum diff no Dockerfile entre a base e o estado final desta frente; re-scan não aplicável antes da consolidação. A verificação T2.C4 permanece bloqueada e deve ser repetida se outra frente alterar o Dockerfile; 2026-09-24.
Evidência: comparação Git sem saída; base `52565edf5d81beb593c1cb78f8d23e49820cd076`.
Dependência externa / responsável / ação para desbloquear: consolidador / repetir build e Trivy caso Dockerfile mude durante integração.
