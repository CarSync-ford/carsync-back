# Status — 01-pipeline

Spec / base SHA / responsável / repositório: `01-pipeline` / `52565edf5d81beb593c1cb78f8d23e49820cd076` / Claude Code / `CarSync-ford/carsync-back`

## T1.C1

Checkpoint: T1.C1
Estado: VERIFICADO
Requisito: R01, R06
Arquivos e teste/comando: `docs/security/sec-2026/01-pipeline/REPORT.md`; revisão do Mermaid e da tabela etapa/entrada/risco/resultado.
Resultado observado e data: fluxo inicial e fluxo proposto documentados sem declarar scanners ou deploy como executados; fluxo atualizado com separação de container scan em PR/main e transferência segura de imagem aprovada para o deploy sem rebuild; 2026-09-25.
Evidência: `docs/security/sec-2026/01-pipeline/REPORT.md`; base `52565edf5d81beb593c1cb78f8d23e49820cd076`.
Dependência externa / responsável / ação para desbloquear: nenhuma.

## T2.C1

Checkpoint: T2.C1
Estado: VERIFICADO
Requisito: R02
Arquivos e teste/comando: `.github/workflows/deploy.yml`; `gh pr checks 33`.
Resultado observado e data: SAST (Semgrep) aprovado nos commits `6947874` (29s) e `c7af363` (28s, run 36074219523); regras `p/ci`, `p/java` e `p/secrets`; 2026-09-24 / 2026-09-25.
Evidência: https://github.com/CarSync-ford/carsync-back/actions/runs/36074219523/job/107882024803
Dependência externa / responsável / ação para desbloquear: nenhuma.

## T2.C2

Checkpoint: T2.C2
Estado: VERIFICADO
Requisito: R03
Arquivos e teste/comando: `pom.xml`; `src/main/java/br/com/sprint1/challenge/config/SecurityConfig.java`; `mvn -B clean test -Dspring.profiles.active=test`; GitHub Actions run 36074219523.
Resultado observado e data: SCA remoto aprovado com sucesso no run 36074219523 (commit c7af363) em 4m39s no PR #33. Migração para Spring Boot 4.1.1 (Framework 7.0.9, Security 7.1.1, Data JPA 4.1.1, Jackson 3.1.5, PostgreSQL JDBC 42.7.13, Flyway 12.4.0, Hibernate 7.4.5.Final, Tomcat 11.0.26 override e Swagger UI WebJar 5.32.15 com DOMPurify 3.4.13) resultou em scan aprovado conforme gate CVSS >= 7; não implica ausência de todas CVEs salvo relatório comparativo comprovado, com supressões vazias e gate CVSS >= 7 mantidos. Histórico de bloqueios dos runs 36020010271 e 36046716450 preservado. Build local aprovado com 212 testes verdes; 2026-09-25.
Evidência: https://github.com/CarSync-ford/carsync-back/actions/runs/36074219523/job/107882152970; artefato `dependency-check-report`.
Dependência externa / responsável / ação para desbloquear: nenhuma para validação remota; scan local continua dependente de `NVD_API_KEY`.

## T2.C3

Checkpoint: T2.C3
Estado: VERIFICADO
Requisito: R04
Arquivos e teste/comando: `.github/workflows/deploy.yml`, `.gitleaks.toml`, `.gitleaksignore`; Gitleaks `8.30.1` local e GitHub Actions run 36074219523.
Resultado observado e data: 151 commits analisados localmente, zero vazamentos e fixture sintética validada com exit code 1. Execução remota no GitHub Actions confirmada com sucesso em 15s no PR #33 (run 36074219523); 2026-09-25.
Evidência: https://github.com/CarSync-ford/carsync-back/actions/runs/36074219523/job/107883323257; configurações `.gitleaks.toml` e `.gitleaksignore`.
Dependência externa / responsável / ação para desbloquear: nenhuma.

## T2.C4

Checkpoint: T2.C4
Estado: BLOQUEADO
Requisito: R05
Arquivos e teste/comando: `.github/workflows/deploy.yml`; `docs/security/sec-2026/01-pipeline/check-container.sh`; Python PyYAML; `Dockerfile` atualizado.
Resultado observado e data: Workflow atualizado com job `container-scan` (`needs: test`), para eventos de PR e push na main, com Trivy Action (`ed142fd0673e97e23eac54620cfb913e5ce36c25`, severidades HIGH/CRITICAL, exit-code 1, ignore-unfixed true), registro de ID da imagem e upload de relatório via artifact com `if: always()`. Transferência da imagem aprovada configurada via `docker save` para push na main (`compression-level: 0`, `if-no-files-found: error`). Smoke test `check-container.sh` simplificado validou a rejeição de ID divergente (PASS); pré-condição de acesso ao socket Docker bloqueada retornou exit code 2 com diagnósticos gravados em diretório durável (`blocked.log`, `docker-info.log`); propagação imediata do status de saída do Trivy garantida sem mascarar reprovação. Dockerfile atualizado: base `eclipse-temurin:21-jre-alpine-3.24@sha256:1a29e1fe337eb28b5bec30f0ee8ed29f0ff80ab6f75dcf9313efe82911065a52`, `apk add --no-cache --upgrade 'libexpat>=2.8.5-r0'`, Application Insights agent 3.7.10 com checksum `93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec`. Execução remota do novo job aguarda trigger de CI; nenhum digest inventado; 2026-09-25.
Evidência: `.github/workflows/deploy.yml`; `docs/security/sec-2026/01-pipeline/check-container.sh`; `Dockerfile`; teste de ID divergente aprovado e saída de pré-condição bloqueada confirmada com exit code 2.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor / autorizar execução remota no Actions para rodar o job `container-scan` com Dockerfile corrigido; liberação do socket Docker necessária para execução de scan local completo.

## T3.C1

Checkpoint: T3.C1
Estado: BLOQUEADO
Requisito: R01–R06
Arquivos e teste/comando: `.github/workflows/deploy.yml`, `docs/security/sec-2026/01-pipeline/REPORT.md`, `docs/security/sec-2026/01-pipeline/check-container.sh`.
Resultado observado e data: Pipeline integrado configurado com 5 gates obrigatórios (`needs: [test, sast, sca, secret-scan, container-scan]`). Deploy restrito a push na main, sem rebuild de JAR/Docker, baixando artefato da imagem com `actions/download-artifact` fixada por SHA verificado (`d3f86a106a0bac45b974a628896c90dbdf5c8093 # v4.3.0`) e validando ID do container antes de qualquer credencial ACR/Azure. Digest de publicação capturado estritamente e persistido em `deploy-evidence/published-digest.txt`. Separação estrita entre verificação de PR e deploy em nuvem: a especificação e o aceite da frente não exigem execução de deploy cloud real. O PR #33 executou testes, SAST, SCA e secrets com sucesso (run 36074219523) com deploy ignorado. PR #34 executou testes, SAST, SCA, secrets verdes e Trivy reprovado (run 36142306403) com deploy skipped. Execução remota do novo job `container-scan` com Dockerfile corrigido aguarda trigger no Actions; ausência de deploy real e de digest no registry mantida explícita; 2026-09-25.
Evidência: `.github/workflows/deploy.yml`; run 36074219523; run 36142306403; `check-container.sh`; `REPORT.md`.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor / autorizar execução do novo workflow no Actions para validação do `container-scan` com Dockerfile corrigido.

## T3.C2

Checkpoint: T3.C2
Estado: EM_EXECUCAO
Requisito: R05
Arquivos e teste/comando: `git diff 41d2227 -- Dockerfile`.
Resultado observado e data: Dockerfile alterado em relação à base `41d2227`: base fixada por digest, libexpat 2.8.5-r0, Application Insights agent 3.7.10. Re-scan obrigatório após alteração do Dockerfile; não executado ainda. 2026-09-25.
Evidência: `git diff 41d2227 -- Dockerfile` mostra alterações.
Dependência externa / responsável / ação para desbloquear: GitHub Actions / mantenedor / executar workflow remoto em PR atualizado; re-scan do Trivy na imagem corrigida.
