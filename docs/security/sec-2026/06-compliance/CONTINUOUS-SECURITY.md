# Plano de segurança contínua — Rotinas operacionais

- **Data da publicação:** 2026-09-26
- **Objetivo:** Estabelecer rotinas periódicas de segurança preventiva, auditoria de acesso e resposta contínua integradas à operação da solução CarSync.

---

## 1. Rotina 1: Revisão contínua de dependências e supply chain (SCA)

| Atributo | Especificação |
|---|---|
| **Objetivo** | Prevenir a introdução ou permanência de bibliotecas Java/Maven e imagens base de contêiner contendo vulnerabilidades conhecidas (CVEs). |
| **Ferramentas ativas** | Dependabot (varredura contínua de repositório), OWASP Dependency-Check (no pipeline Maven CI) e Trivy (varredura de imagem Docker). |
| **Procedimento** | 1. Execução automática em todo Pull Request e push para `main`.<br>2. Verificação semanal automática de advisories de segurança via GitHub Security Advisories.<br>3. Triagem de dependências com CVSS >= 7.0.<br>4. Abertura e merge prioritário de PRs de atualização em branch isolada. |
| **Periodicidade** | **Contínua / Em cada PR** para checagens automatizadas; **Semanal** para triagem manual de novos alertas abertos pelo Dependabot. |
| **SLA de correção** | Crítica (CVSS 9.0–10.0): 48 horas.<br>Alta (CVSS 7.0–8.9): 7 dias.<br>Média (CVSS 4.0–6.9): 30 dias. |
| **Responsável** | Mantenedor do repositório / Engenheiro de DevSecOps. |
| **Evidência gerada** | Histórico de PRs do Dependabot, logs de execução do step `Dependency-Check` no workflow GitHub Actions (`01-pipeline/REPORT.md`). |

---

## 2. Rotina 2: Testes contínuos de segurança (SAST e regressão de API)

| Atributo | Especificação |
|---|---|
| **Objetivo** | Identificar falhas de codificação, injeções, configurações inadequadas e regressões em controles de autenticação/autorização antes do merge. |
| **Ferramentas ativas** | Semgrep (SAST), Gitleaks (Secret Scanning) e suíte automatizada de testes JUnit 5 / Spring Security Test. |
| **Procedimento** | 1. O desenvolvedor abre PR contra a branch `main`.<br>2. O workflow CI executa o Gitleaks em todo o histórico de commits do branch.<br>3. O Semgrep executa rulesets específicos para Java, Spring Security e OWASP Top 10.<br>4. Execução dos testes automatizados (unitários, integração e regressivos de segurança: validação de token, HMAC, rate limit, sanitização de erros).<br>5. O merge é bloqueado automaticamente caso qualquer teste falhe ou segredo seja detectado. |
| **Periodicidade** | **Gatilho automático por commit e PR**; reavaliação de novas regras SAST trimestralmente. |
| **Responsável** | Equipe de Desenvolvimento Backend e revisores de código (Code Reviewers). |
| **Evidência gerada** | Status dos checks do GitHub no PR (212 testes aprovados), relatório Semgrep SARIF e logs do Gitleaks. |

---

## 3. Rotina 3: Auditoria periódica de permissões e controle de acesso (RBAC)

| Atributo | Especificação |
|---|---|
| **Objetivo** | Garantir que o princípio do menor privilégio seja respeitado em todas as camadas (Cloud Azure, Repositório GitHub e Perfis da Aplicação). |
| **Escopo avaliado** | 1. **Azure:** Assinatura `7fd8132e-7c9a-4b4d-a191-04b21ecc968c`, resource group `carsync-dev` (roles Owner, Contributor, Reader).<br>2. **GitHub:** Colaboradores, permissões de branch protection na branch `main` e segredos do repositório.<br>3. **Aplicação / Banco:** Tabela `users`, atribuição de roles (`ROLE_USER`, `ROLE_ANALYST`) e correspondência de perfis de domínio. |
| **Procedimento** | 1. Extração do relatório de atribuições RBAC do Azure via `az role assignment list`.<br>2. Revisão da lista de membros com acesso de escrita/admin no GitHub.<br>3. Consulta à base de dados para auditar contas inativas há mais de 90 dias com perfil de analista.<br>4. Revogação imediata de privilégios de colaboradores desligados ou que mudaram de função.<br>5. Registro em ata de conformidade. |
| **Periodicidade** | **Trimestral** (ou imediatamente após desligamento de membros da equipe). |
| **Responsável** | Administrador da Organização Cloud / Líder Técnico do Projeto. |
| **Evidência gerada** | Log de auditoria do Azure Activity Log e registro de revisão de acessos na documentação do projeto. |

---

## 4. Rotina 4: Simulação de resposta a incidentes (Tabletop Exercise)

| Atributo | Especificação |
|---|---|
| **Objetivo** | Testar a prontidão da equipe, a eficácia dos alertas configurados no Azure Monitor e o conhecimento do fluxo de resposta em 5 etapas sem causar disrupção operacional. |
| **Metodologia** | Simulação de mesa (Tabletop) baseada em cenários de risco reais (ex: surto de requisições maliciosas em `/api/v1/auth`, vazamento suspeito de chave HMAC ou indisponibilidade de contêiner). |
| **Procedimento** | 1. Definição do cenário de exercício.<br>2. Acionamento do fluxo formal documentado em `docs/security/sec-2026/05-observability/INCIDENT-RESPONSE.md`.<br>3. Verificação do tempo de detecção (triagem KQL em `AppTraces` / `AppRequests`).<br>4. Validação das ações de contenção teóricas (bloqueio por IP, rotação de chave HMAC, isolamento de contêiner ACA).<br>5. Elaboração de Relatório Pós-Incidente (Post-Mortem) com lições aprendidas e planos de ação. |
| **Periodicidade** | **Semestral**. |
| **Responsável** | Coordenador de Incidentes (Incident Commander) e equipe técnica de sustentação. |
| **Evidência gerada** | Documento de exercício de mesa e validação KQL registrados em `docs/security/sec-2026/05-observability/TABLETOP.md`. |
