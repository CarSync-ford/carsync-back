# Phase 8 — Criptografia local e MQTT/TLS nos componentes Ford

**Estado:** nova spec para lacunas explícitas. Localização dos componentes e evidências externas ainda precisa ser confirmada.
**Origem:** `SEC-REQUIREMENTS.md:2,25,30,35–36,45,60,62` (CRYPTO, IOT e integração MON/OWASP/LGPD).
**Contrato:** `.specs/README.md`. Não criar mobile, firmware, broker ou modelo ML fictício para declarar requisito cumprido.

## INT-1 — Localizar componentes e pontos de integração [Low]

**Entradas:** arquitetura/documentação existente e repositórios Ford disponibilizados pelo responsável.
**Saída:** inventário na atividade 2 de `docs/security/SEC-DELIVERY.md`.

Preencher `componente | repositório/caminho real | tecnologia | responsável | dados locais | transporte | fonte de logs/métricas | evidência disponível | bloqueio` para API, mobile, IoT, dados e ML.

1. Identificar onde dados pessoais, credenciais, telemetria e localização são persistidos localmente.
2. Identificar cliente MQTT, broker real, ambiente de teste, certificados e política de autenticação/acesso disponíveis. Registrar caminhos, nunca valores secretos.
3. Identificar componente ML real; não confundir regra heurística de churn com modelo/serviço ML sem confirmação.
4. Se faltarem repositório, acesso ou decisão de arquitetura, registrar `BLOQUEADO`, informação necessária e responsável a confirmar. Preparação continua válida, mas requisito não está implementado nem N/A.

**Aceite:** cada componente localizado ou bloqueio explícito. Antes de CRYPTO-1/IOT-1, substituir indicações genéricas abaixo por arquivos reais do componente escolhido; não inventar caminhos.

## CRYPTO-1 — Proteger persistência local real [High]

**Pré-condição:** INT-1 identifica dado sensível realmente persistido e sua plataforma. O enunciado não define qual armazenamento; escolher ocorrência real do projeto, registrar justificativa e limites da cobertura.
**Arquivos-alvo:** módulo de persistência local e configuração/teste do componente identificado; caminhos preenchidos antes da execução.

1. Inventariar gravação/leitura do dado e remover persistência desnecessária. Demonstrar criptografia no dado que precisa permanecer local; ausência de dados por si só não é evidência de criptografia.
2. Priorizar API segura nativa: Android Keystore, iOS Keychain ou recurso equivalente da plataforma real, com material de chave fora do arquivo cifrado. Não adicionar biblioteca se recurso nativo/instalado resolve.
3. Se necessário cifrar arquivo/campo sem recurso nativo adequado, usar biblioteca criptográfica padrão e criptografia autenticada, como AES-GCM: chave aleatória, nonce único por cifragem conforme biblioteca, autenticação validada na leitura. Não implementar cifra própria, chave fixa no código, nonce reutilizado ou fallback plaintext.
4. Definir armazenamento/acesso da chave, formato/versionamento mínimo do dado e comportamento quando chave estiver indisponível ou tag inválida. Não logar plaintext/chave nem regenerar chave silenciosamente sobre dados existentes.
5. Se houver dados antigos plaintext, planejar conversão validada sem perda: preservar original até validar escrita/leitura cifrada, tratar falha/rollback e limpar cópia plaintext apenas após verificação e autorização adequada. Não criar política global de retenção.
6. Deixar teste executável no componente: round-trip; plaintext ausente do artefato persistido; adulteração rejeitada; chave errada/ausente falha de forma segura; dado anterior preservado se migração falhar.

**Aceite:** código real, teste e print/trecho sanitizado na atividade 2, com plataforma, dado protegido e gestão da chave explicados. BCrypt, HMAC, TLS, mascaramento ou simples afirmação de TDE não substituem demonstração de criptografia local.

**Fora de escopo:** nova plataforma KMS, novo armazenamento, criptografia de todos os bancos sem necessidade ou SDK Gemini.

## IOT-1 — Configurar MQTT sobre TLS [High]

**Pré-condição:** INT-1 localiza cliente/broker real e ambiente autorizado. Reutilizar broker e biblioteca existentes, sem impor fornecedor/cloud novo.
**Arquivos-alvo:** configuração do cliente MQTT, configuração do listener/broker e testes do componente identificado.

1. Configurar endpoint TLS suportado (tipicamente 8883, conforme broker), TLS 1.2+ e validação da cadeia e hostname/SAN do servidor.
2. Proibir `trustAll`, validação de hostname desligada, certificado aceito sem confiança e fallback para MQTT plaintext. CA privada de teste pode ser usada, instalada explicitamente no truststore de teste; não distribuir chave privada de produção.
3. Manter autenticação por dispositivo/cliente já suportada e secrets fora do código. mTLS pode ser usado se já for arquitetura escolhida; não é requisito adicional obrigatório.
4. Restringir publicação/assinatura aos tópicos necessários por cliente conforme política existente/aprovada. Não inventar tópicos operacionais Ford; registrar nomes reais sem PII.
5. Onde não houver cliente legado autorizado, desabilitar listener plaintext no ambiente da demonstração. Mudança no broker compartilhado exige autorização e análise de consumidores; não causar indisponibilidade por spec.
6. Não implementar nova ingestão/ponte MQTT no backend HTTP apenas para cumprir checklist se transporte real ocorre em outro componente.

**Aceite:** trecho cliente/broker e explicação técnica na atividade 2; conexão positiva segura e restrições de acesso demonstradas, sem senha/certificado privado nos anexos.

## IOT-2 — Testes positivos e negativos [High]

**Dependência:** IOT-1, ambiente de teste isolado e identidades sintéticas.

Teste reproduzível com biblioteca/ferramenta já usada pelo projeto:

| Caso | Resultado esperado |
|---|---|
| CA confiável, hostname correto, credencial válida e tópico permitido | Publicação/assinatura funciona por TLS |
| CA desconhecida ou certificado inválido/expirado | Handshake rejeitado |
| Hostname incompatível | Conexão rejeitada, sem bypass |
| Credencial inválida | Autenticação rejeitada |
| Tópico não autorizado | Operação negada |
| Tentativa plaintext no listener desativado | Conexão recusada; sem fallback automático |

Não testar negação de serviço, outros dispositivos ou produção. Se broker não permitir simular caso, registrar limite e usar fixture isolada, identificada como teste, não evidência do ambiente real.

**Aceite:** comandos/teste e resultados anexados; falhas não expõem segredo e geram sinal sanitizado para phase-4. Deixar teste executável, não apenas captura manual.

## INT-2 — Fechar integração de observabilidade e compliance [Low]

**Dependência:** INT-1; CRYPTO/IOT fornecem evidências conforme disponibilidade.

1. Encaminhar fontes reais de logs/métricas mobile/IoT/ML para MON-1, sem criar endpoint de telemetria adicional se canal atual basta.
2. Encaminhar armazenamento/transporte/dados para inventário LGPD e Mobile Top 10 em phase-5.
3. Registrar fronteiras de confiança e limitações na revisão STRIDE.
4. Conferir que a entrega inclui arquitetura e componentes externos, e não só backend Java.

**Aceite:** referências cruzadas coerentes; nenhum requisito externo marcado concluído apenas por existência desta spec.

## Checklist

- [ ] INT-1: componentes e caminhos reais identificados; bloqueios atribuídos.
- [ ] CRYPTO-1: criptografia local com testes/evidências.
- [ ] IOT-1: MQTT/TLS configurado sem bypass.
- [ ] IOT-2: testes positivos/negativos reproduzíveis.
- [ ] INT-2: observabilidade, LGPD, Mobile Top 10 e STRIDE integrados.
