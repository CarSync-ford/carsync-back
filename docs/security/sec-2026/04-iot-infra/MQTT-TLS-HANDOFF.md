# Handoff MQTT/TLS para o componente efetivo

Este roteiro não é evidência de implementação. Ele deve ser preenchido e executado no repositório/ambiente IoT oficial após autorização do responsável. Estado atual: **BLOQUEADO**.

## 1. Identificação obrigatória antes de executar

Registrar sem segredos:

```text
IOT_REPO=<URL ou caminho oficial>
IOT_BASE_SHA=<SHA imutável>
IOT_WORKTREE=<worktree sec-2026/04-iot-infra no repo IoT>
MQTT_CLIENT_PATH=<arquivo real>
MQTT_TEST_PATH=<teste real>
MQTT_HOST=<hostname de teste autorizado>
MQTT_TLS_PORT=<listener TLS real; não presumir 8883>
MQTT_CA_FILE=<caminho da CA pública/de teste>
MQTT_ALLOWED_TOPIC=<tópico sintético permitido, sem PII>
OWNER=<responsável nominal>
ENVIRONMENT=<local isolado/teste/cloud>
```

Não continuar se o hostname, a CA, o ambiente ou a autorização não estiverem definidos. Nunca copiar chave privada, senha ou token para este repositório/evidência.

## 2. Configuração mínima a confirmar no cliente real

Adaptar à biblioteca existente, sem introduzir outro stack apenas para a demonstração:

- transporte MQTT sobre TLS (`mqtts` ou opção TLS equivalente) para o listener real;
- TLS 1.2 ou superior;
- cadeia validada contra truststore/CA explícita ou truststore do sistema aprovado;
- hostname/SAN validado; `check_hostname=true` ou equivalente;
- modo de verificação obrigatório (`CERT_REQUIRED` ou equivalente), sem `trustAll`, callback permissivo ou certificado autoaceito;
- autenticação existente mantida e secrets injetados por mecanismo aprovado, fora do código, logs e argumentos capturados;
- publicação/assinatura limitada aos tópicos necessários pela ACL existente/aprovada;
- nenhuma tentativa automática de `mqtt://`, listener plaintext ou retry com validação desabilitada.

No broker, confirmar que o listener usado apresenta cadeia completa e protocolo compatível. Desabilitar listener plaintext somente com autorização e análise dos consumidores; não modificar broker compartilhado nesta frente sem aprovação.

## 3. Pré-validação de cadeia e hostname (sem credenciais)

Após preencher variáveis no ambiente autorizado:

```bash
openssl s_client \
  -connect "${MQTT_HOST}:${MQTT_TLS_PORT}" \
  -servername "${MQTT_HOST}" \
  -verify_hostname "${MQTT_HOST}" \
  -verify_return_error \
  -CAfile "${MQTT_CA_FILE}" \
  </dev/null
```

Aceite desta etapa: código de saída zero e `Verify return code: 0 (ok)`. Sanitizar IPs/hostnames privados e detalhes internos antes de anexar. Esta etapa valida TLS, mas **não substitui** a conexão MQTT positiva.

## 4. Conexão MQTT positiva pelo cliente do projeto

Executar o teste existente (ou criar teste no repo IoT em seu worktree) usando identidade e payload sintéticos. O teste deve:

1. carregar hostname, porta, CA e credencial do ambiente, sem imprimir segredo;
2. habilitar verificação de cadeia e hostname explicitamente;
3. conectar pelo listener TLS;
4. publicar/assinar somente `MQTT_ALLOWED_TOPIC`, conforme o fluxo real;
5. falhar se a biblioteca tentar downgrade/fallback;
6. registrar apenas versão TLS, resultado de verificação, operação permitida e timestamp sanitizados.

Aceite de T2.C1: conexão MQTT concluída, operação autorizada confirmada, trecho da configuração real e saída sanitizada vinculados ao SHA do repo IoT. Um `openssl s_client` verde sozinho, HTTPS da API ou broker local inventado não encerram R12.

## 5. Evidência a devolver

- repo, base SHA, commit e caminhos reais de cliente/broker/teste;
- biblioteca e versões relevantes;
- ambiente (`local isolado`, `teste` ou `cloud`) e autorização;
- protocolo/porta observados e origem da confiança;
- trecho de configuração sem valores secretos;
- comando do runner e resultado positivo sanitizado;
- responsável nominal e limites residuais.

Depois da execução, atualizar `STATUS.md` de `BLOQUEADO` para `VERIFICADO` apenas se a conexão MQTT efetiva satisfizer todos os critérios.
