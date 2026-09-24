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
Evidência: `.github/workflows/deploy.yml`; Semgrep action `713efdd345f3035192eaa63f56867b88e63e4e5d`; nenhuma execução remota declarada.
Dependência externa / responsável / ação para desbloquear: ambiente com Semgrep ou Docker acessível / mantenedor do ambiente / executar `docker run --rm -v "$PWD:/src" semgrep/semgrep semgrep scan --config p/ci --config p/java-spring --config p/secrets --sarif --output /src/semgrep.sarif /src`.

## T2.C2

Checkpoint: T2.C2
Estado: BLOQUEADO
Requisito: R03
Arquivos e teste/comando: `pom.xml`, `.github/dependabot.yml`, `.github/workflows/deploy.yml`, `dependency-check-suppressions.xml`; `mvn -B -DskipTests org.owasp:dependency-check-maven:9.0.0:check`.
Resultado observado e data: YAML e XML válidos. Dependency-Check iniciou, mas não produziu análise: NVD respondeu HTTP 403, seguido por `NoDataException: No documents exist`; 2026-09-24. Dependabot atualiza versões; Dependency-Check analisa vulnerabilidades e falha em CVSS >= 7.
Evidência: configuração registrada `dependency-check-suppressions.xml` sem supressões; plugin OWASP `9.0.0`; saída local resumida neste STATUS.
Dependência externa / responsável / ação para desbloquear: acesso ao feed NVD e, preferencialmente, `NVD_API_KEY` / mantenedor do CI / configurar segredo e repetir o comando.
