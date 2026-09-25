# Handoff de segurança — web e mobile

Destinatários: pessoa desenvolvedora frontend ou agente implementador. Base backend auditada: `ceca50ded9c46d1dbc16c2ab1f17217055935e94`, 2026-09-23. **Contrato observado, não declaração de backend sem falhas.** Spec executável: `.specs/sec-2026/03-frontends.md`; protocolo: `.specs/sec-2026/EXECUTION.md`.

## 1. O que cabe aos fronts

A spec histórica `.specs/phase-2-auth-hardening.md` pede: “Front/Mobile: Testar integração após Fase 2 (auth flow muda)”. Essa verificação de integração serve como **regressão necessária às alterações efetivamente realizadas** no backend, não como lista independente de requisitos além do enunciado. O enunciado exige criptografia local (R07) e Mobile Top 10; a integração é meio de validar que as alterações de autenticação não quebram o cliente.

- Verificar login, refresh, expiração e tratamento de erros no aplicativo realmente entregue, contra a **versão backend integrada**.
- Demonstrar **criptografia local** dos dados sensíveis persistidos (SEC-REQUIREMENTS, atividade 2).
- Entregar evidências para Mobile Top 10 e sinais de monitoramento mobile.
- Validação de entrada, assinatura/finalidade do JWT e autorização **continuam no backend**. Esconder botão ou decodificar JWT no cliente não substitui autorização server-side.

Certificate pinning não aparece no enunciado nem nas specs auditadas. Não implementá-lo como cobrança adicional desta entrega. TLS válido continua necessário; nunca ignorar certificado/hostname inválido.

## 2. Antes de alterar código

1. Identificar os repositórios web/mobile de entrega, stack, SHA e responsável. Criar um worktree por spec em cada repositório alterado; registrar a base.
2. Identificar a URL HTTPS da API no ambiente autorizado. Não usar URL inventada nem presumir que `localhost` no dispositivo é o computador do desenvolvedor.
3. Inventariar tokens, dados pessoais, telemetria e localização em memória, storage, cache, logs e backups do cliente.
4. Ler o contrato abaixo e, quando disponível, `docs/security/sec-2026/02-api/CONTRACT.md` da versão backend integrada. Este último será criado pela frente 02; não existe como evidência nesta rodada.

Há um **exemplo**, não frontend oficial confirmado, em `test/ford-test-main/ford-test-main/FORDTESTE/fordretain/03_mobile/fordretain-app/services/api.ts`: AsyncStorage, HTTP localhost:8000, `/auth/login`, `senha`, `access_token`. Esse contrato não é o da API Spring. Não trocar o exemplo silenciosamente nem considerá-lo prova de integração.

## 3. Contrato HTTP observado

Enviar `Content-Type: application/json` e `Accept: application/json`. Usar corpo da resposta para obter tokens; não depender de ler o header Authorization via CORS. Campos vêm de `AuthDtos.java`; rotas de `AuthController.java`.

| Operação | Requisição | Resposta esperada pelo contrato |
|---|---|---|
| Login | `POST /api/v1/auth` com `{"email":"conta-de-teste@example.invalid","password":"<senha-da-conta-de-teste>"}` | 200, `{"token":"<access>","refreshToken":"<refresh>"}` |
| Renovação | `POST /api/v1/auth/refresh` com `{"refreshToken":"<refresh-atual>"}` | 200, novo par `{token,refreshToken}` |
| Recurso protegido | Rota existente autorizada ao perfil; header `Authorization: Bearer <token>` | 2xx se permitido; rejeição se não autenticado/sem permissão |
| Troca de senha, se a UI já expõe | `POST /api/v1/auth/change-password`, bearer access e `{"currentPassword":"...","newPassword":"..."}` | 204 pelo contrato; depende de correção backend descrita abaixo |

Email deve satisfazer `@Email` e `@LowercaseEmail`; password do login tem tamanho 6–20 na base auditada. Não alterar senha, aplicar trim ou lowercase a ela. Pode orientar/normalizar email conforme comportamento aprovado do app. Validação de cadastro pode ser mais estrita: não derivar política de senha nova apenas das regras do login.

A API não retorna `access_token` ou `expires_in`. Não inventar esses campos. Claim `exp` pode ajudar a UX, mas decodificação local não verifica autenticidade nem substitui resposta do servidor. Não fixar TTL em 30 dias: há incoerência conhecida entre configuração e persistência, a ser tratada pela frente 02.

**Erros:** 400 significa payload inválido; 401 deve ser tratado como credencial/sessão inválida; 403 como acesso negado; 429 como limite de requisições. Verificar respostas reais da versão integrada e registrar diferenças. Não converter 403 ou 429 em tentativa de refresh. Erro de rede/5xx não é prova de senha inválida e não deve iniciar loop de login.

## 4. Fluxo de sessão esperado no cliente

1. Login válido: guardar o par de maneira coerente; não registrar tokens, senha ou conteúdo sensível em console/analytics/crash reports.
2. Enviar somente o **access token** como bearer. Refresh vai apenas no corpo de `/auth/refresh`, nunca como bearer de recursos.
3. Ao detectar expiração/401 em recurso protegido, coordenar uma renovação por sessão. Requisições concorrentes aguardam a mesma renovação; não reutilizam o refresh antigo em paralelo.
4. Renovação válida: substituir **ambos** os tokens antes de liberar novas requisições. Tratar falha ao persistir sem deixar combinação de tokens antigos/novos; invalidar estado local se necessário.
5. Repetir requisição original no máximo uma vez, apenas quando seguro. Não repetir automaticamente uma escrita que possa já ter sido processada por falha ambígua de rede; manter a estratégia de idempotência existente.
6. Refresh rejeitado: limpar sessão e dados vinculados que não devam persistir; pedir login. Não tentar renovar novamente a própria chamada de refresh, login ou endpoint público.
7. Logout local: limpar tokens, cache privado e estado de usuário; cancelar/invalidar respostas em voo para que não restaurem sessão antiga. Não afirmar revogação server-side: não foi identificado endpoint de logout nesta API.
8. Em web com abas, coordenar renovação se compartilharem sessão; em mobile, testar retorno do background/reinício sem recuperar token antigo. Adaptar à plataforma, sem introduzir infraestrutura nova.

## 5. Armazenamento e criptografia local

### Mobile nativo/híbrido

- Usar mecanismo seguro já disponível no stack: Keychain no iOS; armazenamento cifrado com chave protegida no Android Keystore. Em stack Expo, avaliar o módulo de secure storage já instalado; não presumir que exista.
- Não guardar tokens/dados sensíveis em AsyncStorage, Preferences, SQLite ou arquivo **sem proteção**. Secure storage é apropriado para pequenos segredos; para volumes maiores usar mecanismo cifrado da plataforma/stack, com chave fora do arquivo cifrado.
- Não embutir chave de criptografia no bundle nem guardar chave junto ao ciphertext. Verificar política de backup, exportação e acessibilidade do storage conforme a plataforma usada.
- Se migrar token antigo do storage inseguro, remover cópia antiga após persistência segura; se a migração falhar, encerrar sessão com segurança, sem perder outros dados do usuário.
- Minimizar retenção local de PII, telemetria e localização. Testar limpeza ao logout/troca de conta. Não exigir persistência sensível só para “mostrar criptografia”.

### Web

- Não usar localStorage/sessionStorage/IndexedDB para tokens sob alegação de criptografia automática. Criptografar token com chave também entregue ao JavaScript não protege contra XSS no mesmo contexto.
- Com o contrato bearer atual, preferir sessão em memória quando viável, assumindo novo login no reload. Essa opção minimiza persistência, **não é evidência de criptografia local**.
- Se o produto exige sessão persistente, registrar decisão de arquitetura com backend antes de mudar contrato. Cookie HttpOnly/Secure depende de suporte server-side e estratégia CSRF; não existe automaticamente porque o frontend o deseja, nem é nova obrigação desta spec.
- Evidência de criptografia local pode vir do aplicativo mobile que efetivamente armazena dados; explicar aplicabilidade no web sem alegar equivalência falsa.

### Como provar

Em ambiente de teste, apresentar trecho de implementação, commit, plataforma/versão e captura sanitizada. Verificar ausência de plaintext nos locais antes usados, recuperação autorizada após reinício quando prevista e limpeza ao logout. Não tentar extrair chaves reais de produção; usar dados sintéticos. BCrypt no servidor, HTTPS, HMAC ou criptografia do banco cloud **não comprovam criptografia local do cliente**.

## 6. Limitações backend que o front não deve esconder

| Lacuna na base auditada | Ação do frontend |
|---|---|
| Filtro aceita refresh/reset como bearer por não distinguir finalidade | Nunca explorar isso como integração; enviar apenas access. Registrar teste negativo e bloquear aceite de JWT seguro até correção 02 |
| `/auth/**` público e principal String versus `UserDetails` | Troca de senha/MFA podem falhar; reportar status real. Não contornar removendo autenticação |
| Lockout pode sofrer rollback transacional | Não afirmar bloqueio garantido; testar com conta sintética e encaminhar resultado à 02 |
| Reset não envia email e não comprova single-use | UI não deve afirmar email efetivamente enviado como fato; usar mensagem genérica. Não criar SMTP/tela nova para a rubrica |
| MFA tem endpoints, mas login não exige segundo fator | Não anunciar autenticação com MFA end-to-end. Se UI existente o expõe, registrar bloqueio; não criar fluxo presumido |
| JWT TTL e persistência refresh podem divergir | Não codificar prazo fixo; respeitar rejeição e versão corrigida |

Rotas opcionais existentes, somente para integrações já expostas: forgot-password `{email}` (202); reset-password `{token,newPassword}` (204); mfa/enable (resposta `{secret,qrCodeUri}`), mfa/verify `{code}` (204), mfa/disable (204), todas sob `/api/v1/auth`. Esses códigos são contrato declarado, não prova de fluxo completo. Nunca registrar secret MFA/URI QR. Não ampliar escopo com novas telas.

**HMAC:** o backend possui `X-HMAC-Signature`, separado do JWT. Não distribuir segredo HMAC global em app público, JavaScript, build env do frontend ou armazenamento local. Se o ambiente exigir HMAC para chamadas do cliente público, registrar bloqueio com a frente 02 para definição da fronteira de confiança. Não desativar controle nem inventar BFF/proxy nesta entrega sem decisão explícita. Auth é dispensado no filtro atual; isso não prova acesso às demais rotas.

## 7. Matriz mínima de testes do cliente

Executar usando o runner existente ou um roteiro reproduzível no aplicativo real; mocks ajudam desenvolvimento, mas não encerram o aceite integrado.

| Caso | Resultado a verificar |
|---|---|
| Credenciais válidas | UI autenticada, par recebido, recurso permitido carrega |
| Credenciais inválidas/conta bloqueada | Mensagem neutra, sem token salvo nem enumeração |
| Payload inválido | 400 tratado, sem crash e sem armazenamento parcial |
| Access expirado | Uma renovação e, se segura, uma repetição; novo par em uso |
| Duas requisições com sessão expirada | Renovação coordenada, sem replay concorrente do refresh antigo |
| Refresh expirado/revogado/reutilizado | Sessão encerrada, sem loop |
| Papel sem permissão | 403 tratado sem refresh nem acesso indevido; servidor impõe a regra |
| Rate limit | 429 tratado sem tempestade de retries; respeitar Retry-After se fornecido |
| Offline/5xx | Erro de conectividade/serviço distinto de credencial inválida |
| Logout durante refresh | Resposta tardia não reabre sessão |
| Reinício/troca de conta | Persistência conforme decisão, sem dados privados da conta anterior |
| Inspeção storage/logs/backup de teste | Sem senha/token/PII desprotegidos; prova de criptografia quando persistidos |
| Troca de senha/reset/MFA, se já expostos | Resultado real documentado; falha backend vira bloqueio, não workaround no cliente |

## 8. Entrega para backend, monitoramento e consolidação

Produzir na pasta `docs/security/sec-2026/03-frontends/`: `REPORT.md`, `STATUS.md`, capturas sanitizadas e referências ao código/testes do repo real. Registrar:

- repo e SHAs frontend/backend, plataforma/build, ambiente, data e executor;
- caso executado, ação, esperado, observado, PASSOU/FALHOU/BLOQUEADO e evidência;
- mecanismo de storage/gestão de chave, dados protegidos e limites;
- categorias Mobile Top 10 aplicáveis e lacunas, sem declarar conformidade automática;
- sinais mobile já disponíveis (falha de autenticação/rede, erro de app), origem e redaction para a frente 05; não criar endpoint de telemetria por suposição;
- bloqueios com responsável e menor ação necessária.

Não anexar JWTs, refresh tokens, senhas, chaves, QR MFA, emails reais ou localização de pessoas. Sem acesso ao frontend real, este documento é handoff concluído; implementação e evidências continuam **pendentes**.
