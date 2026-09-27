# Guia de implementação de segurança — Frontend (Web e Mobile)

- **Destinatários:** Desenvolvedores e mantenedores dos clientes Web e Mobile do projeto CarSync (Ford Challenge).
- **Data de publicação:** 2026-09-26
- **Ambiente de referência da API:** `https://api.carsync.me/` (Azure Container Apps / East US)
- **Versão backend:** Integrada na branch `sec-2026/fechamento` (`origin/main`)

---

## 1. Contrato e rotas de autenticação

O backend teve suas rotas de autenticação protegidas e padronizadas. Os clientes devem adequar suas chamadas aos seguintes contratos:

### 1.1 Rotas públicas (POST anônimo autorizado)
Apenas as seguintes 4 rotas aceitam chamadas sem o cabeçalho `Authorization: Bearer`:

1. **Login:** `POST /api/v1/auth`
   - Payload: `{"email": "usuario@exemplo.com", "password": "senhaDoUsuario"}`
   - Resposta (200):
     ```json
     {
       "token": "<jwt-access-token>",
       "refreshToken": "<uuid-refresh-token>"
     }
     ```
   - **Atenção:** Os nomes dos campos são `token` (access token, TTL 15 min) e `refreshToken` (refresh token, TTL 7 dias). A API **não** utiliza os nomes `access_token` ou `expires_in`.
2. **Renovação de Sessão:** `POST /api/v1/auth/refresh`
   - Payload: `{"refreshToken": "<uuid-refresh-token>"}`
   - Resposta (200): Novo par `{"token": "...", "refreshToken": "..."}`.
3. **Esqueci a Senha:** `POST /api/v1/auth/forgot-password`
   - Payload: `{"email": "usuario@exemplo.com"}`
   - Resposta (202): Vazia (`Accepted`). A resposta é 202 indistinta contra enumeração de e-mails. O front deve sempre informar: *"Se o e-mail informado estiver cadastrado, você receberá as instruções de recuperação."*
4. **Redefinição de Senha:** `POST /api/v1/auth/reset-password`
   - Payload: `{"token": "token-de-15-min-recebido-por-email", "newPassword": "NovaSenhaSegura123!"}`
   - Resposta (204): Vazia (`No Content`).

### 1.2 Rotas sensíveis restritas (Exigem Bearer Token obrigatório)
As seguintes rotas **não são públicas**. Chamadas sem o cabeçalho `Authorization: Bearer <token>` receberão **HTTP 401 Unauthorized**:

- `POST /api/v1/auth/change-password`: Payload `{"currentPassword": "...", "newPassword": "..."}`.
- `POST /api/v1/auth/mfa/enable`: Retorna `{"secret": "...", "qrCodeUri": "otpauth://totp/..."}`.
- `POST /api/v1/auth/mfa/verify`: Payload `{"code": "123456"}`.
- `POST /api/v1/auth/mfa/disable`: Desativa MFA para o usuário logado.

---

## 2. Ciclo de vida da sessão e renovação de tokens

Para garantir uma navegação fluida sem deslogar o usuário a cada 15 minutos:

### 2.1 Interceptor HTTP para renovação atômica (Silent Refresh)
O frontend (via Axios Interceptor, Fetch wrapper, OkHttp ou Alamofire) deve interceptar respostas **HTTP 401** de rotas de negócio:

1. Ao receber 401 em uma rota protegida:
   - Pausar temporariamente novas requisições.
   - Enviar requisição única `POST /api/v1/auth/refresh` com o `refreshToken` atual.
2. Ao receber 200 do refresh:
   - **Sobrescrever imediatamente** tanto o `token` quanto o `refreshToken` no armazenamento seguro.
   - O backend invalida o refresh token anterior no banco; **reutilizar o refresh antigo gerará 401**.
   - Reexecutar as requisições que estavam em espera utilizando o novo `token`.
3. Se o refresh retornar 401:
   - O refresh token expirou ou foi revogado.
   - Limpar todos os dados da sessão local e redirecionar para a tela de Login.

### 2.2 Controle de requisições concorrentes
Implemente uma flag ou fila de espera durante o refresh:
```typescript
let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: any) => void }> = [];

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach(prom => {
    if (error) prom.reject(error);
    else prom.resolve(token!);
  });
  failedQueue = [];
}
```

---

## 3. Criptografia local obrigatória no Mobile (Requisito R07)

> **REGRA DE SEGURANÇA MANDATÓRIA (OWASP Mobile Top 10 M9:2024):**
> É expressamente proibido armazenar tokens JWT, credenciais, segredos TOTP ou dados cadastrais em texto claro em `SharedPreferences` padrão, `UserDefaults` simples, `AsyncStorage` desprotegido ou bancos de dados sem cifragem.

### 3.1 Implementação no Android
Utilize a biblioteca oficial **AndroidX Security Crypto** com chave mestre protegida pelo hardware do dispositivo:

```kotlin
// Android: EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "carsync_secure_session",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Salvar tokens
sharedPreferences.edit()
    .putString("auth_token", token)
    .putString("refresh_token", refreshToken)
    .apply()
```

### 3.2 Implementação no iOS
Utilize o **Keychain Services** da Apple com controle de acessibilidade restrito:

```swift
// iOS: Keychain Services
let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrService as String: "br.com.carsync.session",
    kSecAttrAccount as String: "auth_token",
    kSecValueData as String: token.data(using: .utf8)!,
    kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
]
SecItemAdd(query as CFDictionary, nil)
```

### 3.3 Implementação em React Native / Flutter
Utilize wrappers nativos consolidados:
- **React Native:** `react-native-keychain` ou `expo-secure-store`.
- **Flutter:** `flutter_secure_storage` configurado com `encryptedSharedPreferences: true` no Android.

---

## 4. Integração com MFA TOTP (RFC 6238)

Para as telas de perfil do usuário:

1. **Ativação:**
   - Chamar `POST /api/v1/auth/mfa/enable`.
   - Renderizar o QR Code a partir da string `qrCodeUri` (utilizar biblioteca geradora de QR Code SVG/Canvas).
   - Exibir também a chave em texto (`secret`) para cópia manual pelo usuário caso não consiga escanear a tela.
2. **Confirmação:**
   - Exibir campo de 6 dígitos numéricos.
   - Enviar `POST /api/v1/auth/mfa/verify` com `{"code": "123456"}`.
   - Ao receber 204, exibir feedback visual de que o segundo fator está ativo.

---

## 5. Assinatura HMAC-SHA256 (`X-HMAC-Signature`)

Endpoints de recursos de negócio protegidos (analytics, leads, stock, vehicle-assistant e churn) exigem o cabeçalho:
`X-HMAC-Signature: <hash-sha256-hex>`

- **Cálculo:** HMAC-SHA256 do corpo da requisição (payload JSON bruto) utilizando o segredo compartilhado (`API_HMAC_SECRET`).
- Em requisições `GET` (sem corpo), o cálculo utiliza string vazia `""`.

---

## 6. Tratamento de limites de taxa (Rate Limiting HTTP 429)

- Quando o backend responder com **HTTP 429 Too Many Requests**, o cabeçalho `Retry-After: <segundos>` indica o tempo de espera necessário.
- **Na UI:**
  - Desabilitar botões de submissão temporariamente.
  - Exibir contador regressivo ao usuário: *"Muitas tentativas. Aguarde X segundos antes de tentar novamente."*
