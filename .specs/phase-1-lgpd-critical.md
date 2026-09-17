# Phase 1 — Perfis exigidos e mapeamento LGPD

**Estado:** plano de adequação. Código já possui USER/ANALYST, analytics mascarados e jobs de retenção; isso não atende automaticamente aos perfis exigidos nem comprova LGPD.
**Origem:** `SEC-REQUIREMENTS.md:28–29,62` (RBAC, LGPD).
**Contrato:** `.specs/README.md`.

## RBAC-1 — Definir matriz dos três perfis [High]

**Entradas:** controllers existentes, `SecurityConfig`, `JwtAuthenticationFilter`, serviços de usuário/auth, `User`, `UserType`, migrações de `user_type` e DTOs de cadastro/edição.
**Saída:** matriz na atividade 2 de `docs/security/SEC-DELIVERY.md`.

1. Inventariar endpoints reais e permissões atuais; separar autenticação de autorização.
2. Representar explicitamente **Brigadista, Gestor e Administrador**. Proposta de nomes técnicos: `BRIGADISTA`, `GESTOR`, `ADMINISTRADOR`, respeitando prefixo `ROLE_` do Spring no ponto apropriado.
3. Mapear `método/rota | operação | Brigadista | Gestor | Administrador | restrição de objeto | justificativa`. Não criar endpoints de negócio para preencher matriz.
4. O enunciado nomeia perfis, mas não define suas permissões. Obter aprovação da matriz e do destino de usuários USER/ANALYST antes de migrar dados ou conceder acesso. Não converter ANALYST em Administrador nem USER em Gestor por suposição.
5. Inspecionar cadastro público (`POST /api/v1/user`) e atualização de usuário: cliente não pode escolher/promover papel privilegiado. Definir perfil inicial permitido na matriz aprovada.

**Aceite:** três perfis presentes, operações existentes cobertas e decisão de transição explícita. Sem aprovação das permissões, RBAC-2 fica `BLOQUEADO`; não usar N/A para substituir perfis pedidos.

## RBAC-2 — Aplicar autorização e transição [High]

**Pré-condição:** RBAC-1 aprovado; coordenar CLEAN-3 da phase-7.
**Arquivos-alvo:** entidade/repositório/serviço de perfis e usuários, auth/JWT, controllers com `@PreAuthorize`, migração nova em `src/main/resources/db/migration/` e par H2 em `src/test/resources/db/migration/h2/`.

1. Reutilizar modelo de perfis existente, incluindo tipo real do ID. Não copiar `INSERT` antigo com ID inteiro se banco usa UUID.
2. Criar os três perfis por migração incremental com próximo número livre. Não reescrever V2/V6 ou outra migração histórica.
3. Aplicar matriz em rotas existentes e restrições de objeto quando necessárias ao acesso autorizado. Não basta trocar nome do role em uma única annotation.
4. Impedir alteração de papel por payload não autorizado e elevação via cadastro, edição ou claims JWT manipulados.
5. Tratar tokens de papéis antigos: nenhum USER/ANALYST pode manter acesso por regra ampla após corte. Definir expiração/revogação/reautenticação compatível com mecanismo real, sem prometer invalidação de access JWT stateless que não exista.
6. Migrar usuários apenas segundo mapeamento aprovado, preservando vínculos e integridade. Remover papel ANALYST conforme CLEAN-3; decidir destino de USER sem inventar quarto perfil de negócio permanente.
7. Adicionar testes parametrizados no padrão existente: cada perfil tem sucesso apenas onde permitido; sem autenticação recebe 401; papel inadequado recebe 403; cadastro/edição não elevam privilégio; token legado não contorna nova matriz.

**Aceite:** matriz demonstrada por testes positivos/negativos, sem abertura transitória de endpoints. Produção exige plano autorizado de migração e rollback seguro; teste local não comprova usuários migrados.

## DATA-1 — Inventário LGPD mínimo [High]

**Entradas:** entidades/DTOs, logs, integrações, dados mobile/IoT/ML identificados em phase-8.
**Saída:** atividade 4 do documento consolidado, complementada em phase-5.

Tabela obrigatória: `classe de dado | campos reais | origem | finalidade | base legal a validar | acesso | armazenamento/transporte | compartilhamento | retenção/descarte | proteção/evidência | lacuna`.

Cobrir separadamente:

- **Dados pessoais:** identificadores, cadastro e contatos realmente tratados.
- **Telemetria:** sinais do veículo/dispositivo e possibilidade de vinculação a pessoa.
- **Localização:** coordenadas ou outros dados de localização efetivamente coletados; identificar componente responsável mesmo se externo ao backend.

Reutilizar mascaramento e minimização úteis. Não chamar mascaramento reversível/parcial de anonimização garantida. Não criar ANALYST, endpoints de analytics de PII ou jobs de descarte como condição artificial de LGPD. Prazos legais dependem de finalidade/base legal; 30 dias e 5 anos do plano antigo não são requisitos SEC.

**Aceite:** três classes analisadas; ausência de componente ou política é lacuna explícita. Nenhum dado de produção é apagado por esta tarefa.

## Retirada do plano anterior

- Criação de ANALYST e endpoints exclusivos de listagem mascarada: substituída pela matriz de três perfis; remoção do excedente em CLEAN-3.
- `DataRetentionService` e política automática arbitrária: retirada planejada em CLEAN-4.
- Não remover mascaramento dos endpoints preservados nem fazer registros com `deleted_at` voltarem a aparecer. Segurança de dados continua obrigatória.

## Checklist

- [ ] RBAC-1: matriz e transição aprovadas.
- [ ] RBAC-2: três perfis e testes de autorização.
- [ ] DATA-1: dados pessoais, telemetria e localização inventariados.
- [ ] Remoções de ANALYST/retention coordenadas com phase-7, sem exposição de PII.
