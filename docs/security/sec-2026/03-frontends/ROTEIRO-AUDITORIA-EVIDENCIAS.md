# Roteiro de auditoria e evidências de segurança — Frontend (Web e Mobile)

- **Finalidade:** Guia prático de testes, auditoria técnica e geração das evidências exigidas pelo programa de segurança (requisitos R07, R10, R14 e R20).
- **Data:** 2026-09-26
- **Pasta de destino das evidências visuais:** `docs/security/sec-2026/03-frontends/captures/`

---

## 1. Matriz de requisitos cobrida pelo Frontend

| ID | Requisito do programa | Critério de auditoria no Frontend |
|---|---|---|
| **R07** | Criptografia local | Comprovação inequívoca de que tokens e dados do usuário estão armazenados de forma cifrada no storage nativo do dispositivo (Android Keystore / iOS Keychain). |
| **R10** | JWT seguro no cliente | Demonstração de renovação transparente de sessão (silent refresh), tratamento de expiração e eliminação de tokens no logout. |
| **R14** | Evidências reais | Código-fonte dos interceptors e telas, prints reais das interfaces/armazenamento e explicações técnicas. |
| **R20** | OWASP Mobile Top 10 | Mitigação comprovada das categorias M1 (Credenciais), M3 (Autenticação), M5 (Comunicação) e M9 (Armazenamento inseguro). |

---

## 2. Cenários de auditoria e procedimentos de teste

### Cenário 1: Auditoria de Criptografia Local (Requisito R07 / OWASP M9)
- **Objetivo:** Provar que os dados gravados no dispositivo móvel não podem ser lidos em texto claro por um invasor com acesso físico ou aplicativo malicioso.
- **Procedimento no Android (emulador ou dispositivo em modo depuração):**
  1. Abrir o aplicativo CarSync e realizar login com conta de teste.
  2. Acessar o terminal do sistema operacional e executar:
     ```bash
     adb shell
     run-as br.com.carsync
     cat shared_prefs/carsync_secure_session.xml
     ```
  3. **Resultado esperado:** As chaves e os valores XML devem conter exclusivamente hashes Base64 e dados cifrados (AES-256-GCM). Nenhuma string legível como tokens em texto claro, e-mail ou CPF pode aparecer no arquivo.
- **Procedimento no iOS:**
  1. No simulador iOS ou dispositivo, inspecionar o container de dados da aplicação.
  2. Confirmar que a chave `kSecClassGenericPassword` foi persistida com atributo `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
- **Evidência exigida:** Captura de tela do terminal exibindo o conteúdo XML/Keychain cifrado (`r07-encrypted-storage.png`).

### Cenário 2: Auditoria do Fluxo de Renovação de Tokens (Requisito R10)
- **Objetivo:** Comprovar a rotação automática do refresh token e renovação da sessão sem impacto para a experiência do usuário.
- **Procedimento de teste:**
  1. Efetuar login no aplicativo e monitorar o tráfego de rede (Chrome DevTools / Flipper / Charles Proxy).
  2. Aguardar a expiração do token de acesso (15 minutos) ou alterar localmente o token em memória para um valor inválido.
  3. Tentar acessar uma tela protegida (ex: `/api/v1/customers/1/360` ou `/api/v1/leads`).
  4. Observar a sequência de requisições disparadas pelo interceptor HTTP.
- **Resultado esperado na auditoria de rede:**
  - Requisição 1: Rota de negócio -> HTTP 401 Unauthorized.
  - Requisição 2 (automática em background): `POST /api/v1/auth/refresh` -> HTTP 200 OK (com novos tokens).
  - Requisição 3 (repetição): Rota de negócio com o novo Bearer token -> HTTP 200 OK.
- **Evidência exigida:** Captura do painel de rede (Network tab) mostrando as 3 chamadas sequenciais (`token-refresh-flow.png`).

### Cenário 3: Auditoria da Configuração de MFA (Autenticação Multifator)
- **Objetivo:** Comprovar o suporte a múltiplos fatores de autenticação diretamente na interface.
- **Procedimento de teste:**
  1. Navegar até as configurações de perfil / segurança.
  2. Clicar em "Ativar Autenticação em Duas Etapas".
  3. Verificar a renderização do QR Code oficial.
  4. Escanear no app Google Authenticator / Microsoft Authenticator e inserir o código de 6 dígitos.
  5. Confirmar a ativação com sucesso.
- **Evidência exigida:** Captura da tela do aplicativo exibindo o QR Code gerado e a tela de confirmação (`mfa-setup-flow.png`).

### Cenário 4: Auditoria de Logout e Limpeza de Estado
- **Objetivo:** Garantir que o encerramento da sessão expurga os tokens do armazenamento seguro do cliente.
- **Procedimento de teste:**
  1. Clicar no botão "Sair" / "Logout".
  2. Reinspecionar o armazenamento local (`shared_prefs` ou Keychain).
  3. Tentar navegar para telas anteriores utilizando o botão "Voltar" do sistema operacional ou histórico do navegador.
- **Resultado esperado:** Armazenamento de credenciais zerado/limpo; navegação bloqueada redirecionando para a tela de autenticação.
- **Evidência exigida:** Print da tela de login após logout e verificação de storage vazio (`logout-cleanup.png`).

### Cenário 5: Auditoria de Resiliência a Rate Limiting (HTTP 429)
- **Objetivo:** Validar que a interface trata adequadamente tentativas excessivas de requisições.
- **Procedimento de teste:**
  1. Clicar repetidamente no botão de submissão de login (mais de 5 vezes em menos de 1 minuto).
  2. Verificar o recebimento de HTTP 429 pelo backend.
- **Resultado esperado:** O aplicativo não trava; exibe mensagem informativa com contagem regressiva baseada no cabeçalho `Retry-After` e desativa o botão de submissão.
- **Evidência exigida:** Captura da interface exibindo a notificação de rate limit (`rate-limit-ui-feedback.png`).

---

## 3. Padrão de nomenclatura e sanitização das evidências

As evidências visuais coletadas devem seguir este padrão estrito antes de serem anexadas ao repositório:

1. **Localização dos arquivos:** Salvar na pasta `docs/security/sec-2026/03-frontends/captures/`.
2. **Nomes dos arquivos:**
   - `r07-encrypted-storage-android.png`: Dump do XML cifrado no Android.
   - `r07-encrypted-storage-ios.png`: Prova de gravação no Keychain no iOS.
   - `token-refresh-network-flow.png`: Painel de rede demonstrando o refresh atômico.
   - `mfa-setup-screen.png`: Interface com o QR Code e código TOTP.
   - `rate-limit-ui-feedback.png`: Interface tratando o HTTP 429.
3. **Regras de Sanitização (Zero PII e Zero Vazamento de Segredos):**
   - Ocultar senhas reais ou e-mails pessoais de desenvolvedores.
   - Mascarar tokens JWT exibidos em tela ou logs (manter apenas prefixos truncados com asteriscos).
   - Ocultar identificadores de contas de teste confidenciais.

---

## 4. Checklist final de entrega do Frontend

- [ ] Biblioteca de armazenamento seguro configurada no projeto (AndroidX Security Crypto / iOS Keychain).
- [ ] Interceptor de renovação atômica (silent refresh) tratando HTTP 401 e requisições concorrentes.
- [ ] Interface de ativação de MFA integrada com QR Code e validação TOTP.
- [ ] Cabeçalho `X-HMAC-Signature` enviado nas rotas de negócio que exigem assinatura.
- [ ] Tratamento amigável e bloqueio temporário em respostas HTTP 429.
- [ ] As 5 evidências em imagem capturadas, sanitizadas e salvas em `docs/security/sec-2026/03-frontends/captures/`.
