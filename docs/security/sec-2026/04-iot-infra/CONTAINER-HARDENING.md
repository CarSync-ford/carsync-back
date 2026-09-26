# Evidência de hardening do container — T3.C1

## Escopo e versões

- Base desta frente: `41d2227eb7f4382a9e58a4696dad76485088bb76`.
- Dockerfile original desta base: SHA-256 `5f1f9c1afc4cf94bf00f35b3e45f5b9605acf29a503d30618c56ec60896d3b97`.
- Correção coordenada já implementada pela frente 01: commit `7f735d39fa0eecfa187ddfff1aba931c1b602347`, blob Dockerfile `db0f46830fc1b9750401c3e1b6bcd3733394f52f`.
- Validação remota: GitHub Actions run [36148042299](https://github.com/CarSync-ford/carsync-back/actions/runs/36148042299), job [Container Scan](https://github.com/CarSync-ford/carsync-back/actions/runs/36148042299/job/108114377733), concluído com sucesso em 2026-09-25.
- O run reporta `headSha=7f735d...`; o artefato registra `GIT_SHA=80770908be0fec32935057380a688ce775135372`, SHA de checkout/merge do evento de pull request. Ambos são preservados para não fingir identidade de build.

T3.C1 reutiliza essa implementação/evidência da frente 01 em vez de produzir uma segunda alteração concorrente no Dockerfile. O Dockerfile desta branch 04 permanece o da base até a integração serializada.

## Build observado

| Verificação | Ambiente | Resultado |
|---|---|---|
| `mvn -B -DskipTests package` | local, worktree 04, Java 21 | `BUILD SUCCESS`; JAR repackaged produzido |
| `docker version`/daemon | local | CLI 29.7.2 presente; acesso ao socket negado; usuário fora do grupo `docker`; nenhum build local alegado |
| `Build with Maven` | GitHub Actions run 36148042299 | sucesso |
| `Build Docker image` | GitHub Actions, Docker 28.0.4 | sucesso no Dockerfile corrigido pela frente 01 |
| smoke `java -javaagent:/opt/agent.jar -version` com `--network none` | imagem remota, sem override de usuário | comando concluiu; Java 21.0.12 executou; agente carregou, mas sua inicialização de telemetria falhou por ausência de connection string |
| Trivy HIGH/CRITICAL com `ignore-unfixed` | mesma imagem | zero vulnerabilidades reportadas em Alpine 3.24.2 e `app/app.jar`; coluna Secrets `-` (não escaneada) |

O smoke não é prova de Application Insights operante: a saída real contém `No connection string provided`. Isso é coerente com segredo injetado somente em runtime e ajuda a mostrar que a credencial não foi embutida, mas telemetria/cloud não foi testada nesta frente.

## Controles explicados

### Execução sem root

1. O Dockerfile cria grupo/usuário de sistema `spring`.
2. O artefato é colocado em `/app/app.jar` e recebe ownership `spring:spring`.
3. A última instrução de identidade é `USER spring:spring`, sem estágio posterior que retorne a root.
4. O smoke remoto não usa `--user`; portanto executa com o usuário configurado pela imagem. O UID/GID numérico não foi impresso no artefato e não é inventado aqui.
5. `-XX:+DisableAttachMechanism` reduz a superfície de attach dinâmico da JVM. Os limites de memória evitam consumo não limitado pelo heap, embora não substituam limites do runtime.

### Minimização de cópia e secrets

- O único `COPY` do Dockerfile é `COPY target/*.jar app.jar`; `.env`, código-fonte, chaves e diretórios do host não são copiados para a imagem por essa instrução.
- Não há `ARG` ou `ENV` para senha, token, chave ou connection string no Dockerfile original/corrigido.
- A inspeção do JAR local não encontrou `.env`, chaves SSH, PEM/KEY/P12/PFX ou `applicationinsights.json`; `application.yml` contém referências externas `${...}` em vez de valores coletados do ambiente local.
- O smoke remoto registrou ausência de connection string do Application Insights, sem revelar valor.
- O relatório Trivy **não** executou secret scanning (`Secrets: -`). A aprovação de Gitleaks no mesmo run cobre o repositório Git, não deve ser descrita como varredura do filesystem da imagem.
- Sem acesso ao daemon local e sem imagem exportada pelo run de PR, `docker image inspect` de `Config.Env`, history e permissões numéricas não pôde ser repetido nesta frente.

### Integridade e vulnerabilidades da imagem coordenada

A frente 01 corrigiu três defeitos antes do re-scan:

- base `eclipse-temurin:21-jre-alpine-3.24` fixada por digest;
- `libexpat` atualizada para versão corrigida;
- agente Application Insights atualizado para 3.7.10 e validado por SHA-256 durante o build.

O artefato remoto confirma checksum do agente `93a70c8f...2eb0ec` e Trivy sem achados HIGH/CRITICAL conforme a política configurada. Não se declara ausência de todas as CVEs, porque o gate ignora achados sem correção e limita severidades.

## Coordenação e risco residual

- Nenhuma mudança foi feita em `Dockerfile` nesta branch; portanto não há novo conteúdo a notificar à frente 01 para re-scan. A própria frente 01 já executou o re-scan obrigatório no run 36148042299.
- Na integração, deve prevalecer o Dockerfile do commit `7f735d...`; integrar apenas a documentação desta frente sem a correção 01 deixaria a base/tag e o download do agente sem os pins validados.
- Se houver qualquer resolução de conflito ou alteração posterior no Dockerfile, a frente 01 deve executar novo build/Trivy e registrar o novo SHA.
- `docker-compose.dev.yml` executa uma ferramenta de desenvolvimento e não participa da imagem de produção avaliada; não foi alterado.
- Deploy Azure, secrets de runtime e telemetria ativa não foram executados nem inferidos a partir deste build de PR.
