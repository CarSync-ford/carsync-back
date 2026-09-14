# Phase 0 - Quick Wins (1-2 dias)

**Prioridade:** Imediato | **Esforço:** ~0.5 semana

Já implementados no código, apenas validar/ativar em produção.

## Itens

### 1. HMAC enabled por default
- **Arquivo:** `src/main/resources/application.yml:47`
- **Status:** ✅ Implementado (`hmac.enabled: ${HMAC_ENABLED:true}`)
- **Ação:** Confirmar que no ACA a variável `HMAC_ENABLED=true` e `HMAC_SECRET` está configurada como secret/env var

### 2. Security Headers
- **Arquivo:** `src/main/java/br/com/sprint1/challenge/config/SecurityConfig.java:71-76`
- **Status:** ✅ Implementado
- **Headers:**
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: geolocation=(), microphone=()`

### 3. JWT Secret Validation
- **Arquivo:** `src/main/java/br/com/sprint1/challenge/service/impl/JwtServiceImpl.java:27-34`
- **Status:** ✅ Implementado
- **Validação:** `@PostConstruct` lança `IllegalStateException` se secret < 32 chars (256 bits)

### 4. CORS Fail-fast
- **Arquivo:** `src/main/java/br/com/sprint1/challenge/config/SecurityConfig.java:45-51`
- **Status:** ✅ Implementado
- **Validação:** `@PostConstruct` rejeita `CORS_ALLOWED_ORIGINS` vazio ou `*`

### 5. Validar HMAC_SECRET nos secrets/env vars do Container App (ACA)
- **Ação:** Verificar via Azure CLI:
  ```bash
  # Listar secrets no Container App
  az containerapp secret list --name <app-name> --resource-group <rg> -o table
  
  # Adicionar secret no Container App (se ausente)
  az containerapp secret set --name <app-name> --resource-group <rg> --secrets hmac-secret=<valor>
  
  # Ver env vars injetadas no Container App
  az containerapp show --name <app-name> --resource-group <rg> --query "properties.template.containers[0].env" -o table
  ```

## Critério de Pronto

### Implementação e Testes Automatizados (Concluído)
- [x] `HMAC_ENABLED=true` como default em `application.yml` e probes `/actuator/health/**` liberadas sem assinatura (`HmacSignatureFilterTest`, `HmacSignatureFilterIntegrationTest`)
- [x] Security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`) configurados e validados via integração (`SecurityHeadersIntegrationTest`)
- [x] JWT secret validation (mínimo 256 bits / 32 caracteres) com fail-fast no startup testado (`JwtServiceImplIntegrationTest`)
- [x] CORS fail-fast (rejeição de vazio ou `*`) com startup testado (`SecurityConfigCorsIntegrationTest`)
- [x] Suíte de 92 testes automatizados executando com sucesso (`mvn clean test`)

### Validação em Produção / Azure Container Apps (Pós-Deploy)
- [ ] `HMAC_ENABLED=true` confirmado no ambiente de produção (ACA)
- [ ] `HMAC_SECRET` configurado no Container App (`az containerapp secret set`) e mapeado em env var
- [ ] Security headers confirmados em resposta HTTP pública (`curl -I https://<app-domain>/api/v1/health`)
- [ ] Startup em produção validado com origins de CORS e JWT Secret configurados