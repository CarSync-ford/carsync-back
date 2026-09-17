# Phase 4 — Observabilidade, monitoramento e resposta

**Estado:** plano revisado; instrumentação e ambiente precisam ser conferidos.
**Origem:** `SEC-REQUIREMENTS.md:39–51` (LOG, MON, IR).
**Contrato:** `.specs/README.md`.

## Limite

Usar stack de observabilidade já disponível no projeto. Azure Monitor/App Insights é opção, não obrigação de contratar/provisionar serviços. Não criar dashboard comercial, mapa geográfico de IPs ou integração APIM/Cloudflare. Plano de resposta deve funcionar com controles existentes, sem depender de MFA que será removido.

Entregar atividade 3 em `docs/security/SEC-DELIVERY.md`, incluindo prints reais, exemplos de logs sanitizados e fluxo de resposta. Documento sem essas evidências permanece incompleto.

## LOG-1 — Eventos estruturados [High]

**Entradas:** configuração de logging em `src/main/resources/`, handlers de autenticação, rate limit, autorização e operações críticas existentes.

1. Identificar formato/coletor atual. Reutilizar suporte estruturado instalado antes de adicionar dependência.
2. Emitir eventos de login bem-sucedido, falha de login e alteração crítica (por exemplo, troca autenticada de senha ou alteração autorizada de perfil, quando existir).
3. Campos mínimos: timestamp UTC, evento, componente, resultado, identificador de correlação. Identidade técnica do ator apenas quando necessária; não expor email/CPF/localização precisa.
4. Incluir falhas de JWT/autorização/rate limit já disponíveis, sem duplicar logs por camada.
5. Proibir senha, token, cabeçalho Authorization, seed TOTP, chaves e conteúdo sensível. Tratar campos controlados pelo cliente para evitar injeção em logs.
6. Testar estrutura parseável e ausência de segredos com dados sintéticos, usando infraestrutura de testes existente.

**Aceite:** teste produz e valida os três tipos exigidos (login, falha, alteração crítica); exemplos reais sanitizados anexados à atividade 3. Logs textuais existentes não contam automaticamente como estruturados.

## MON-1 — Contrato de métricas e alertas por componente [High]

**Pré-condição:** localizar os componentes reais Ford com phase-8. Se componente externo não estiver disponível, registrar tarefa de integração bloqueada, não N/A.

| Componente | Sinais mínimos propostos | Alerta proposto | Fonte a identificar |
|---|---|---|---|
| API | Volume de falhas de autenticação, respostas 5xx, latência e rate limit | Aumento sustentado de falhas/erros | Logs/métricas da API |
| Mobile | Falhas de autenticação e comunicação segura | Aumento de falhas TLS/autenticação | Telemetria sanitizada do app real |
| IoT | Falhas de autenticação/TLS e desconexões MQTT | Dispositivos sem conexão ou rejeições repetidas | Cliente/broker MQTT real |
| ML | Erros de inferência e rejeições de entrada/acesso | Aumento sustentado de erros/rejeições | Componente de inferência real |

Esses sinais são escolhas mínimas para MON, não nova suíte de analytics. Não criar modelo ML ou aplicação mobile para gerar métricas artificiais.

1. Para cada sinal, registrar nome/campo real, origem, unidade, janela, limiar escolhido e justificativa, destinatário e ação.
2. Reutilizar métricas/logs existentes. Adicionar apenas instrumentação faltante no componente correto.
3. Evitar labels de alta cardinalidade e PII, como token, email, coordenada ou ID por usuário/dispositivo.
4. Testar alerta com amostra sintética identificada como tal; demonstrar recuperação quando condição deixa de existir.

**Aceite:** API/mobile/IoT/ML constam no plano, com fonte real e status de integração; alertas disponíveis têm evidência de teste. Fonte ausente permanece pendência visível.

## MON-2 — Dashboard e consultas [Low]

**Dependência:** LOG-1 e contrato MON-1 com campos reais.
**Arquivos-alvo:** configuração/exportação suportada pela stack existente; `docs/security/SEC-DELIVERY.md`.

1. Criar ou ajustar dashboard de segurança/monitoramento cobrindo sinais MON-1. Quantidade de painéis segue necessidade, sem metas arbitrárias de "3+ alertas" ou "2+ dashboards".
2. Conferir schema do coletor antes de escrever consultas. Não presumir coluna `IP_s` nem que JSON em `Log_s` já esteja extraído.
3. Salvar consultas/exportação para reprodução quando ferramenta permitir.
4. Capturar prints legíveis, com janela temporal, nomes dos sinais e dados sensíveis removidos. Ausência de componente não deve aparecer como zero incidentes.

**Aceite:** consultas executam no schema disponível; prints reais e fontes explicadas na atividade 3. Mock de dashboard não substitui evidência.

## IR-1 — Resposta a incidentes [Low]

**Arquivo-alvo:** atividade 3 de `docs/security/SEC-DELIVERY.md`.

Descrever cinco etapas obrigatórias:

1. **Detecção:** alerta/log e registro de horário, componente e correlação.
2. **Análise:** escopo, severidade, impacto e preservação de evidências sem divulgar PII.
3. **Contenção:** limitar acesso/isolar componente ou revogar sessão conforme recurso realmente disponível e autorização necessária.
4. **Erradicação:** corrigir causa, substituir credenciais comprometidas quando aplicável e remover artefato malicioso.
5. **Recuperação:** restaurar serviço/dados conforme rotina de backup, validar integridade e acompanhar sinais.

Incluir tabela `cenário | sinal | análise | contenção | erradicação | recuperação | responsável`. Cobrir vazamento de credencial, abuso de API e falha de segurança MQTT/TLS com ações suportadas pelo ambiente. Não inventar emails como `security@company.com`, DPO nomeado ou equipe/on-call inexistente; responsável não definido fica pendente.

**Aceite:** fluxo completo e coerente com infraestrutura existente; cada ação indica pré-condição e responsável a confirmar. Exercício operacional pode servir de evidência, mas certificação NIST, plantão e programa de drills não são novas obrigações.

## Checklist

- [ ] LOG-1: logs estruturados e amostras sanitizadas.
- [ ] MON-1: plano API/mobile/IoT/ML, métricas e alertas rastreáveis.
- [ ] MON-2: dashboard e prints reais.
- [ ] IR-1: fluxo das cinco etapas com ações viáveis.
