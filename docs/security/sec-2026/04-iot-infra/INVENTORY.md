# Inventário MQTT/TLS — T1.C1

Verificação realizada em 2026-09-25 sobre `CarSync-ford/carsync-back` na base `41d2227eb7f4382a9e58a4696dad76485088bb76`.

## Resultado

| Item exigido | Resultado observado | Origem da evidência |
|---|---|---|
| Repositório backend | `CarSync-ford/carsync-back` | `git remote -v` |
| Componente IoT oficial | Não identificado | graphify, árvore Git, manifests, histórico e busca textual |
| Repositório/SHA IoT | Não identificado | nenhum submódulo, remote ou handoff IoT versionado |
| Cliente/biblioteca MQTT | Não identificado | sem Paho, Mosquitto, Azure IoT SDK ou dependência MQTT nos manifests/código |
| Broker/hostname | Não identificado | nenhuma configuração de broker encontrada |
| Protocolo/porta efetivos | Não identificados | não há URI `mqtt://`/`mqtts://`, listener 1883/8883 ou equivalente versionado |
| Origem da confiança TLS | Não identificada | não há CA, truststore ou configuração de validação MQTT |
| Tópicos/ACL | Não identificados | nenhuma política de tópico encontrada |
| Responsável IoT | Não identificado nominalmente | sem CODEOWNERS/OWNERS/MAINTAINERS e sem atribuição IoT na documentação |
| Responsável pelo desbloqueio | Mantenedor da entrega integrada CarSync/Ford | governança necessária para indicar o componente oficial e autorizar o teste |

## Evidência negativa e limites

A consulta graphify do worktree continha 1.476 nós e retornou elementos de API/mobile/Cloudflare sem um caminho para cliente ou broker MQTT. Como o grafo versionado não substitui uma inspeção do conteúdo atual, o resultado foi confrontado com busca textual, manifests, branches e histórico. As únicas ocorrências relevantes em todo o histórico foram specs (`.specs/phase-8-local-crypto-iot.md` e a spec atual), não implementação.

A árvore local também continha diretórios de outros trabalhos. Dois candidatos com nomes potencialmente relacionados foram verificados somente para identidade:

- `mobile-dev-iot`: remote pessoal `Enzoreal100/mobile-dev-iot`, sem referência MQTT, CarSync ou Ford no `HEAD`; não há vínculo objetivo com esta entrega.
- `Ford`: remote pessoal `GustavoPasquiniLucas/Ford`, sem referência MQTT/IoT no `HEAD`; não há vínculo objetivo com o componente exigido.

Eles **não** foram tratados como infraestrutura oficial e não foram alterados. Pesquisa web pelo namespace do remote não retornou resultado verificável. A ausência de implementação neste backend não torna R12 não aplicável ao projeto integrado.

## Dados necessários para desbloqueio

1. URL/caminho do repositório oficial do firmware ou cliente IoT e SHA/branch de integração.
2. Nome/contato do responsável pelo componente e autorização para ambiente isolado.
3. Biblioteca e configuração MQTT efetivas, broker/porta, política de autenticação e tópicos permitidos.
4. CA/truststore e hostname de teste, sem chaves privadas ou credenciais de produção.
5. Janela/ambiente em que conexão positiva e rejeições negativas possam ser exercitadas sem impacto compartilhado.

Quando esses dados existirem, registrar a base do repo IoT, criar worktree próprio e executar T2.C1/T2.C2 nele. Não adicionar broker ou ponte MQTT a este backend HTTP apenas para satisfazer o checklist.
