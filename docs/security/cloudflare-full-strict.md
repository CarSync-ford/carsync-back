# Configuração e Validação: Cloudflare Full (Strict)

## 1. Contexto e Motivação de Segurança

No modelo de borda com Cloudflare e Azure Container Apps (ACA), existem três modos principais de operação SSL/TLS:

| Modo Cloudflare | Criptografia Navegador ↔ Cloudflare | Criptografia Cloudflare ↔ Origin (ACA) | Validação do Certificado da Origem | Risco |
|---|---|---|---|---|
| **Flexible** | Sim (HTTPS) | Não (HTTP plaintext) | Nenhuma | **Crítico:** Tráfego interceptável em trânsito no backbone |
| **Full** | Sim (HTTPS) | Sim (HTTPS) | Não (aceita autoassinado/expirado) | **Alto:** Vulnerável a ataques Man-in-the-Middle (MITM) na conexão de origem |
| **Full (Strict)** | Sim (HTTPS) | Sim (HTTPS) | **Sim (valida cadeia de AC pública confiável ou Cloudflare Origin CA)** | **Seguro:** Integridade e confidencialidade ponta a ponta garantidas |

Para atender aos requisitos de conformidade (LGPD art. 46, PCI-DSS e padrões ISO 27001), o modo **Full (Strict)** é mandatório: garante que o Cloudflare só encaminhará tráfego para instâncias do ACA cujo certificado TLS seja autêntico, válido e emitido por Autoridade Certificadora pública ou pelo Cloudflare Origin CA.

---

## 2. Passo a Passo de Configuração

### Passo 1: Configurar Domínio Customizado e Certificado no ACA

No Azure Container Apps, vincular o domínio customizado (ex: `api.carsync.com.br`) utilizando um dos métodos:

#### Opção A: Azure Managed Certificate (Recomendado)
O Azure emite e renova automaticamente um certificado público gratuito via DigiCert:
```bash
# 1. Adicionar o hostname ao Container App
az containerapp hostname add \
  --resource-group rg-carsync-prod \
  --name aca-carsync-backend \
  --hostname api.carsync.com.br

# 2. Emitir o certificado gerenciado gratuito
az containerapp hostname bind \
  --resource-group rg-carsync-prod \
  --name aca-carsync-backend \
  --hostname api.carsync.com.br \
  --environment managed-env-carsync \
  --validation-method CNAME
```

#### Opção B: Cloudflare Origin CA Certificate
Gerar um certificado com validade de até 15 anos no painel Cloudflare (SSL/TLS → Origin Server) e importá-lo no ACA Environment:
```bash
az containerapp env certificate upload \
  --resource-group rg-carsync-prod \
  --name managed-env-carsync \
  --certificate-file origin-cert.pfx \
  --password <pfx-password>
```

### Passo 2: Ativar Full (Strict) no Painel Cloudflare

1. Acessar o painel Cloudflare para o domínio `carsync.com.br`.
2. Navegar para **SSL/TLS** → **Overview**.
3. Selecionar a opção **Full (strict)**.
4. Navegar para **SSL/TLS** → **Edge Certificates**:
   - **Always Use HTTPS:** Ativado (`On`).
   - **Minimum TLS Version:** `TLS 1.3` (ou `TLS 1.2` se clientes legados forem suportados).
   - **Opportunistic Encryption:** Ativado (`On`).
   - **TLS 1.3 0-RTT:** Desativado (`Off`) para prevenir ataques de replay em requisições POST/PUT.
   - **HTTP Strict Transport Security (HSTS):**
     - Status: Ativado
     - Max Age: 12 meses (`31536000` segundos)
     - Include subdomains: Ativado
     - Preload: Ativado

### Passo 3: Ativar Authenticated Origin Pulls (Opcional - Defense in Depth)

Garante que o ACA aceite conexões TLS originadas exclusivamente dos servidores proxy do Cloudflare por meio de autenticação mTLS:
1. No Cloudflare: **SSL/TLS** → **Origin Server** → **Authenticated Origin Pulls** = `On`.
2. Fazer upload do certificado CA do Cloudflare no Ingress do ACA.

---

## 3. Validação e Testes

Executar o script de validação automatizada:

```bash
./infra/cloudflare/verify-origin-ssl.sh api.carsync.com.br
```

### Validações Manuais via Terminal:

1. **Testar handshake e cadeia de certificados:**
   ```bash
   openssl s_client -connect api.carsync.com.br:443 -servername api.carsync.com.br -tls1_3
   ```
   *Critério de Sucesso:* `Verify return code: 0 (ok)`.

2. **Testar cabeçalhos HSTS e resposta via cURL:**
   ```bash
   curl -vI https://api.carsync.com.br/api/v1/health
   ```
   *Critério de Sucesso:*
   - Resposta HTTP 200 OK.
   - Presença de `strict-transport-security: max-age=31536000; includeSubDomains; preload`.
   - Presença de `cf-ray: ...` confirmando passagem pelo Cloudflare Edge.

---

## 4. Troubleshooting de Erros Comuns

- **Error 525 (SSL Handshake Failed):**
  - Causa: Falha de handshake criptográfico entre Cloudflare e ACA.
  - Correção: Verificar se a porta 443 está aberta no ACA e se a versão mínima de TLS no ACA é compatível com Cloudflare (TLS 1.2 / 1.3).
- **Error 526 (Invalid SSL Certificate):**
  - Causa: Cloudflare está em Full (Strict), mas o certificado do ACA expirou, é autoassinado sem CA confiável, ou o Common Name (CN) / SAN não corresponde ao hostname requisitado.
  - Correção: Renovar o certificado gerenciado no ACA ou reemitir o Cloudflare Origin Certificate cobrindo o subdomínio correto.
