# Handoffs — observabilidade e compliance

Data: 2026-09-25. Base backend: `41d2227eb7f4382a9e58a4696dad76485088bb76`.

## Frente 05 — observabilidade

### Estado da fonte

Nenhum cliente, broker, firmware, SDK ou ambiente IoT oficial foi identificado. Portanto, **não existe fonte de logs/métricas IoT vigente comprovada por esta frente**. O owner de 05 não deve copiar nomes abaixo como eventos já implantados.

### Fonte a obter do componente real

| Sinal necessário | Fonte real a confirmar | Uso de resposta |
|---|---|---|
| falha de handshake/cadeia/hostname TLS | callback/logger do cliente e log TLS do broker | detectar configuração expirada/incorreta ou possível interceptação; bloquear downgrade |
| autenticação MQTT rejeitada | callback do cliente e auth log do broker | verificar identidade/credencial, rotação e abuso sem expor secret |
| ACL/tópico negado | log de autorização do broker | conter cliente comprometido/mal configurado e revisar menor privilégio |
| desconexão/reconexões | estado do cliente e métrica de sessão do broker | distinguir instabilidade, indisponibilidade e loop de retry |
| fallback/plaintext tentado | instrumentação de endpoint/tentativas do cliente | incidente de configuração; impedir transmissão e corrigir cliente |

O responsável nominal do IoT deve fornecer: biblioteca/versão, callbacks reais, formato de log, backend de métricas, retenção, ambiente, responsável por alerta e playbook. Limiares e destinos são decisão da frente 05; não foram definidos como vigentes aqui.

### Schema mínimo proposto, não implantado

Quando compatível com o stack real, eventos sanitizados podem conter:

```text
timestamp, environment, component_version, event_type,
tls_version, verification_stage, outcome, retry_count,
correlation_id, device_id_pseudonymized
```

Não registrar usuário/senha, token, chave/certificado privado, payload, VIN bruto, tópico com identificador pessoal, localização, URI com credencial ou stack trace contendo segredo. Hostname/IP interno só deve aparecer em evidência com autorização e sanitização. Falha TLS deve ser registrada antes de qualquer MQTT CONNECT autenticado e não pode disparar retry plaintext.

### Ações para 05

1. Manter IoT como fonte ausente/bloqueada até o handoff do repo real.
2. Após T2.C1/T2.C2, receber os nomes/campos efetivos e amostras sanitizadas.
3. Correlacionar cliente e broker sem usar identificador pessoal direto quando não necessário.
4. Definir alertas sobre falha/retry com baseline real; não apresentar limiar proposto como configurado.
5. Incluir resposta: analisar cadeia/expiração, revogar identidade se comprometida, conter cliente, corrigir trust/ACL, recuperar e validar sem downgrade.

## Frente 06 — compliance, LGPD e riscos

### Estado do inventário

A rubrica cita telemetria e localização, mas esta frente não encontrou payload ou fluxo IoT real. Logo, não se afirma que o projeto coleta ou deixa de coletar qualquer campo específico. Os itens abaixo são perguntas obrigatórias para o owner, não um inventário factual.

### Dados e decisões a confirmar

| Categoria potencial | Confirmar no componente real | Risco a avaliar |
|---|---|---|
| identidade do dispositivo/veículo | vínculo com pessoa, conta, VIN ou frota | rastreabilidade/identificação indireta |
| telemetria do veículo/sensor | campos, granularidade, frequência, finalidade | perfil comportamental, inferência e excesso de coleta |
| localização | precisão, frequência, modo background e necessidade | vigilância, rotina e segurança física |
| diagnóstico e logs | payload, tópicos, IDs, IPs e erros | vazamento em logs/observabilidade |
| credenciais/certificados | lifecycle, associação, revogação e acesso | impersonação do dispositivo e exposição de segredo |

Para cada campo real, 06 deve registrar finalidade, hipótese/base legal aprovada, titular/controlador/operador, origem, destinatários, retenção, descarte, acesso, transferência, direitos e risco residual. Pseudonimização não deve ser descrita automaticamente como anonimização.

### Fronteiras e riscos atuais

- Transporte dispositivo/cliente → broker: segurança não verificada enquanto R12 estiver bloqueado.
- Broker → consumidor/backend: fluxo não identificado; não presumir que esta API Spring ingere MQTT.
- Observabilidade: logs podem transformar IDs, tópicos, payload e localização em nova cópia de dados pessoais.
- Evidência: capturas/PCAPs devem usar identidade/payload sintéticos e ser minimizadas/sanitizadas.
- Continuidade: rotação/revogação de identidade e recuperação de falha TLS dependem do owner IoT.

### Ações para 06

1. Manter a lacuna IoT explícita no STRIDE e checklist, sem marcar N/A.
2. Solicitar arquitetura de dados e payloads ao owner IoT antes do inventário LGPD final.
3. Vincular qualquer evidência T2 futura ao ambiente e SHA corretos, sem anexar dados reais de titulares.
4. Tratar localização como dado de alto impacto e justificar necessidade/granularidade/retenção se existir.
5. Registrar responsável e prazo de resolução; documento/handoff não equivale a controle implantado.
