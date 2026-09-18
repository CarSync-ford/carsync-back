# Backlog e Calendário Operacional de Hardening Contínuo

## 1. Calendário Recorrente de Segurança (Fase 6)

Em conformidade com a Seção 6.4 do plano de segurança e escala da aplicação CarSync, as rotinas contínuas de hardening estão calendarizadas conforme a matriz operacional abaixo:

| Item | Frequência | Agenda (UTC) | Ação / Escopo | Responsável | Evidência de Conclusão |
|---|---|---|---|---|---|
| **Dependency updates** | Semanal | Segunda-feira, 14:00 | Triagem e merge de PRs automatizados do Dependabot (Maven e GitHub Actions) com validação de build e testes | Dev Team | PR aprovado e merged, link dos jobs de CI no ticket semanal |
| **CVE scan** | Mensal | 1º dia útil do mês, 14:00 | Análise de relatórios OWASP Dependency-Check, Trivy (contêineres), Semgrep (SAST) e Gitleaks | SecOps / Dev Team | Relatório sanitizado arquivado, issues de remediação abertas para CVEs CVSS ≥ 7.0 |
| **Permission audit** | Trimestral | 15 de Jan, Abr, Jul, Out, 14:00 | Auditoria de privilégios mínimos: ACA Secrets, Azure RBAC, APIM subscriptions/roles, permissões PostgreSQL | SecOps + Cloud Infra | Matriz de acessos sanitizada, revogação de acessos órfãos e termo de aceite |
| **Penetration test** | Semestral | 15 de Maio e Nov, 14:00 | Teste de intrusão (gray-box) em APIs REST, autenticação, autorização BOLA/BFLA e injeção | SecOps / Empresa Externa | Relatório executivo/técnico, plano de ação e reteste formal |
| **TLS config review** | Semestral | 15 de Jun e Dez, 14:00 | Avaliação de configurações criptográficas, ciphersuites e headers; atingir e manter nota A+ no Qualys SSL Labs | Cloud Infra / SecOps | Relatório SSL Labs A+, verificação de renovação de certificados e HSTS preload |
| **Secret rotation** | Anual | 1º de Outubro, 14:00 | Rotação coordenada de segredos críticos: segredo JWT, chave HMAC, senhas PostgreSQL no Azure Key Vault | SecOps + Dev Team | Registro de rotação sem indisponibilidade, confirmação de invalidação de segredos antigos |
| **STRIDE review** | Anual | 1º de Novembro, 14:00 | Revisão e atualização do modelo de ameaças STRIDE frente a novas funcionalidades e integrações | Arquitetura + SecOps | Documento STRIDE versionado com novas ameaças e controles mitigadores |
| **Incident response drill** | Anual | 1º de Dezembro, 14:00 | Simulação de mesa (tabletop exercise) com cenário de vazamento de dados ou credencial comprometida | Equipe Multidisciplinar (Dev, Sec, DPO, Jurídico) | Ata do simulado, lições aprendidas e atualização do playbook de resposta |

---

## 2. Procedimentos Operacionais Padrão (SOPs)

### SOP 1: Dependency Updates (Semanal)
1. Analisar os PRs criados automaticamente pelo Dependabot no GitHub.
2. Conferir release notes e changelog de dependências para identificar breaking changes.
3. Verificar se o pipeline de CI (`deploy.yml`) executou com sucesso (build, testes unitários, testes de integração e scans).
4. Realizar o merge do PR via squash e atualizar a branch base.

### SOP 2: CVE & Vulnerability Scan (Mensal)
1. Executar o scan localmente ou extrair os relatórios gerados pelos actions de CI:
   - OWASP Dependency-Check: `mvn dependency-check:check`
   - Trivy Container Scan: `trivy image carsync-backend:latest`
2. Classificar vulnerabilidades encontradas:
   - **Crítica / Alta (CVSS ≥ 7.0):** Criação imediata de issue com SLA de remediação de até 7 dias corridos.
   - **Média (CVSS 4.0 - 6.9):** SLA de remediação na sprint seguinte (até 30 dias).
   - **Baixa / Falso Positivo:** Documentar em `dependency-check-suppressions.xml` com justificativa técnica e data de revisão.

### SOP 3: Permission & Access Audit (Trimestral)
1. **Azure RBAC:** Listar atribuições de role na Subscription e Resource Group:
   ```bash
   az role assignment list --resource-group rg-carsync-prod --output table
   ```
2. **PostgreSQL:** Validar se a aplicação utiliza usuário com permissões restritas (DML apenas, sem permissões de DDL no runtime).
3. **ACA Secrets:** Verificar segredos armazenados no Azure Container Apps e garantir que credenciais antigas não permaneçam ativas.
4. **APIM:** Auditar chaves de subscrição ativas e revogar contas inativas há mais de 90 dias.

### SOP 4: Penetration Testing (Semestral)
1. Definir Termo de Autorização e Escopo de Teste com regras de engajamento (ROE).
2. Executar testes cobrindo o OWASP API Security Top 10 (Broken Object Level Authorization, Broken Authentication, Mass Assignment, Rate Limiting).
3. Utilizar ambiente de homologação espelhado com dados sintéticos ou anonimizados.
4. Consolidar achados e realizar reteste obrigatório para itens de severidade Crítica ou Alta.

### SOP 5: TLS & SSL Labs Review (Semestral)
1. Submeter o domínio público da API ao [Qualys SSL Labs](https://www.ssllabs.com/ssltest/):
   - Alvo mandatório: **Nota A+**.
2. Validar parâmetros criptográficos:
   - TLS 1.3 obrigatório (TLS 1.2 aceito se estritamente necessário; SSLv3, TLS 1.0 e 1.1 bloqueados).
   - Cipher suites seguras (preferência para ciphers AEAD como ChaCha20-Poly1305, AES-GCM).
   - HSTS com `max-age` ≥ 31536000 segundos, `includeSubDomains` e `preload`.
   - Certificado com chave RSA ≥ 2048 bits ou ECC P-256/P-384.

### SOP 6: Secret Rotation (Anual)
Procedimento de rotação sem indisponibilidade (Zero-Downtime):
1. **Segredo JWT:**
   - Implementar período de transição aceitando tokens assinados pela chave anterior por até o tempo de expiração do token (15 minutos), enquanto novos tokens são assinados exclusivamente com a nova chave.
2. **Chave HMAC:**
   - Atualizar a chave no cofre de segredos e reiniciar os contêineres de forma sequencial (rolling update).
3. **Credenciais do Banco de Dados:**
   - Criar usuário alternativo no PostgreSQL com as mesmas permissões.
   - Atualizar o secret no Azure Key Vault / ACA Secrets para o novo usuário.
   - Reiniciar a aplicação e, após validação das conexões, revogar a senha do usuário anterior.

### SOP 7: STRIDE Threat Model Review (Anual)
1. Reunir equipe de arquitetura e segurança.
2. Revisar o Diagrama de Fluxo de Dados (DFD) da aplicação.
3. Avaliar as 6 categorias STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).
4. Atualizar o relatório de Threat Model em `docs/security/threat-model.md`.

### SOP 8: Incident Response Tabletop Drill (Anual)
1. Conduzir exercício simulado com a equipe técnica e DPO.
2. Cenário padrão: "Vazamento suspeito de credenciais de serviço ou incidente envolvendo dados pessoais sob LGPD".
3. Avaliar tempo de detecção, contenção de credenciais, análise forense de logs (`SECURITY_VIOLATION`), notificação ao DPO e prazos da ANPD (art. 48 da LGPD).
4. Produzir ata formal com oportunidades de melhoria.

---

## 3. Modelo de Registro de Execução (Ticket de Auditoria)

```text
============================================================
REGISTRO OPERACIONAL DE HARDENING CONTÍNUO
============================================================
Atividade: [Dependency updates | CVE scan | Permission audit | Pentest | TLS review | Secret rotation | STRIDE | Drill]
Data Prevista: YYYY-MM-DD
Data Executada: YYYY-MM-DD HH:mm UTC
Responsável Executante: [Nome / Role]
Aprovador / Revisor: [Nome / Role]
Ambiente / Commit / Imagem: [Hash / Tag]
------------------------------------------------------------
Escopo Analisado:
Resultados Obtidos:
Pendências Identificadas (com severidade, owner e SLA):
Riscos Aceitos / Supressões Justificadas:
Status Final: [ APROVADO | REPROVADO | REQUER AÇÃO ]
Data da Próxima Execução: YYYY-MM-DD
============================================================
```
