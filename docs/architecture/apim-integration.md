# Integração Azure API Management (APIM) - Arquitetura e Operação

## 1. Visão Geral e Objetivos

Com a escala horizontal dos microserviços no Azure Container Apps (ACA), a camada de borda assume papéis críticos de segurança, governança e disponibilidade. A integração com o **Azure API Management (APIM)** resolve a limitação de rate limiting local multi-réplica descrita no [ADR 001](../adr/001-in-memory-rate-limiting.md) e estabelece um perímetro de segurança centralizado.

### Responsabilidades Delegadas ao APIM:
- **Rate Limiting & Throttling Distribuído:** Aplicação centralizada de cotas por IP (`counter-key="@(context.Request.IpAddress)"`) e por chave de subscrição (`rate-limit-by-key`), blindando o backend contra ataques DoS/DDoS.
- **Validação JWT na Borda:** Interceptação e validação de tokens JWT (Microsoft Entra ID ou custom issuer) antes de repassar chamadas ao ACA, descartando requisições não autorizadas com código HTTP 401 sem consumir CPU do container.
- **Injeção de Correlation ID e Rastreabilidade:** Geração ou repasse de cabeçalho `X-Correlation-ID` para observabilidade ponta a ponta.
- **Normalização de Cabeçalhos de Segurança:** Injeção estrita de HSTS, CSP, X-Frame-Options, e remoção de assinaturas de servidor (`Server`, `X-Powered-By`).
- **Isolamento de Origem:** ACA configurado com restrição de ingress para permitir acesso apenas a partir dos IPs do APIM ou via integração VNet.

---

## 2. Topologia de Rede

```
[ Usuário / Cliente ]
         │ (HTTPS TLS 1.3 - Cloudflare Full Strict)
         ▼
[ Cloudflare Edge ]
         │ (HTTPS mTLS / Origin Pull)
         ▼
[ Azure API Management (APIM) ]
   - Rate limiting (100 req/min por IP)
   - JWT Pre-validation
   - Correlation ID Injection
         │ (HTTPS restrito via IP Filtering / VNet)
         ▼
[ Azure Container Apps (ACA Ingress) ]
   - Regra: Permitir APIM IP / Negar outros
   - Env: RATE_LIMIT_ENABLED=false
   - Spring Boot App (CarSync Backend)
```

---

## 3. Passo a Passo de Implementação

### Passo 1: Provisionamento do APIM
Provisionar uma instância de APIM (SKU Developer para homologação, Consumption ou Standard v2/Premium para produção):

```bash
az apim create \
  --name apim-carsync \
  --resource-group rg-carsync-prod \
  --location brazilsouth \
  --publisher-name "CarSync Security Team" \
  --publisher-email "security@carsync.com.br" \
  --sku-name Consumption
```

### Passo 2: Exportação e Importação da OpenAPI Spec

O backend expõe a especificação OpenAPI 3.0 em `/v3/api-docs`. Utilize o script automatizado:

```bash
# Exporta do ambiente local ou do endpoint ACA
./infra/apim/scripts/export-openapi.sh https://aca-carsync-backend.azurecontainerapps.io infra/apim/openapi-spec.json

# Importa a API no APIM
az apim api import \
  --resource-group rg-carsync-prod \
  --service-name apim-carsync \
  --api-id carsync-api \
  --path "/api" \
  --specification-format OpenApiJson \
  --specification-path "infra/apim/openapi-spec.json"
```

### Passo 3: Configuração de Policies Globais/API

O arquivo de policies em `infra/apim/policies/api-policy.xml` contém as regras de rate limiting, validação JWT e segurança.

Aplicação via Azure CLI:
```bash
az apim api policy save \
  --resource-group rg-carsync-prod \
  --service-name apim-carsync \
  --api-id carsync-api \
  --policy-file "infra/apim/policies/api-policy.xml"
```

### Passo 4: Restrição de Ingress no ACA (Isolamento de Origem)

Para garantir que ninguém consiga burlar o APIM acessando a URL direta do Container App (`*.azurecontainerapps.io`), configura-se a restrição de IP no Ingress do ACA:

```bash
./infra/apim/scripts/configure-aca-ingress.sh rg-carsync-prod aca-carsync-backend apim-carsync
```

O script:
1. Extrai o IP público do APIM (`az apim show ... --query publicIpAddresses[0]`).
2. Configura a regra `Allow-APIM-Only` no Ingress do ACA.
3. Altera a variável de ambiente `RATE_LIMIT_ENABLED=false` no ACA.

---

## 4. Desativação do Filtro Interno no Backend

Quando o APIM está ativo:
- O rate limiting é executado na borda (100 req/60s por IP).
- A variável de ambiente `RATE_LIMIT_ENABLED` no contêiner ACA é definida como `false`.
- A classe `RateLimitFilter` verifica `@Value("${rate-limit.enabled:true}")` e passa a requisição imediatamente pelo `filterChain.doFilter(request, response)` sem criar ou consultar buckets em memória, economizando CPU e RAM no container.

Caso ocorra um bypass operacional ou ambiente de desenvolvimento local sem APIM, o valor default `true` reativa a proteção in-memory automaticamente.
