esta última etapa, vocês irão evoluir o trabalho de cibersegurança para um modelo DevSecOps, garantindo que segurança não seja apenas um documento, mas parte contínua do ciclo de desenvolvimento, testes, deploy e operação da solução Ford Challenge.
A Sprint é composta por 4 subetapas, cada uma valendo 0 a 10 pontos, somando 10 pontos (média ponderada das quatro). Todas as entregas devem estar integradas ao projeto Ford, considerando API, mobile, IoT, dados, ML e arquitetura, em um único documento, separado por atividade .

# Pipeline DevSecOps Integrado (Peso 3,0)
## Objetivo: Demonstrar como segurança é incorporada ao pipeline de desenvolvimento, desde o commit até o deploy.
O que deve ser entregue:
• Desenho do pipeline CI/CD com foco em segurança
(GitHub Actions, Azure DevOps, GitLab CI ou equivalente).
• Inclusão de etapas como:
• SAST (Static Application Security Testing) — ex.:
SonarQube, Semgrep.
• SCA (Software Composition Analysis) — ex.:
Dependabot, Snyk.
• Secret Scanning — ex.: GitGuardian, Gitleaks.
• Container Security (se aplicável) — ex.: Trivy.
• Explicação de como cada etapa reduz riscos identificados.
• Entrega esperada: Documento + diagrama + explicação de
como o pipeline seria executado no projeto Ford.


# Segurança em Código e Infraestrutura (Peso 2,5 )
## Objetivo: Evidenciar práticas de segurança aplicadas diretamente no código e na infraestrutura.
O que deve ser entregue:
• Evidências de correções ou melhorias reais no código:
• Criptografia local .
• Hardening de API (rate limit, validação de entrada,
JWT seguro).
• Controle de acesso por perfil (Brigadista, Gestor,
Administrador).
• Segurança MQTT/TLS para IoT.
• Demonstração de IaC Security (Infrastructure as Code), se
aplicável:
• Ex.: Terraform, Dockerfile, Kubernetes YAML com
boas práticas.
• Entrega esperada: Trechos de código, prints, commits,
explicações técnicas.


# Observabilidade, Monitoramento e Resposta (Peso 2,0)
## Objetivo: Mostrar como o sistema detecta, registra e responde a incidentes.
O que deve ser entregue:
• Plano de monitoramento:
• Logs estruturados (login, falhas, alterações
críticas).
• Métricas e alertas (API, mobile, IoT, ML).
• Dashboards (Grafana, Kibana, Azure Monitor ou
equivalente).
• Plano de resposta a incidentes:
• Detecção → análise → contenção → erradicação
→ recuperação.
• Entrega esperada: Documento + prints de dashboards + exemplos de logs + fluxo de resposta.


# Compliance, Riscos e Segurança Contínua (Peso 2,5)
## Objetivo: Demonstrar que o sistema segue boas práticas, normas e políticas de segurança.
O que deve ser entregue:
• Revisão final dos riscos (STRIDE + DevSecOps).
• Mapeamento com normas e boas práticas:
• OWASP ASVS
• OWASP Mobile Top 10
• OWASP API Top 10
• LGPD (dados pessoais, telemetria, localização)
• Plano de segurança contínua:
• Rotina de revisão de dependências.
• Rotina de testes de segurança.
• Rotina de auditoria de permissões.
• Rotina de backup e recuperação (já iniciada na Fase 3).
• Entrega esperada: Documento final consolidado + checklist de conformidade.