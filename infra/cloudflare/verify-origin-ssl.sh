#!/usr/bin/env bash
set -euo pipefail

# Script de verificação e validação de TLS / Cloudflare Full (Strict)
DOMAIN="${1:-carsync.com.br}"

echo "============================================================"
echo " Validando Conexão TLS & Cloudflare Full (Strict) para: ${DOMAIN}"
echo "============================================================"

echo "==> 1. Verificando cabeçalhos HTTP e suporte HTTPS..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -I "https://${DOMAIN}")
echo "    Status HTTP retornado: ${HTTP_CODE}"

echo "==> 2. Verificando cadeia de certificados TLS e versão do protocolo..."
echo | openssl s_client -servername "${DOMAIN}" -connect "${DOMAIN}:443" 2>/dev/null | openssl x509 -noout -issuer -subject -dates

echo "==> 3. Verificando cabeçalho HSTS (Strict-Transport-Security)..."
HSTS_HEADER=$(curl -s -I "https://${DOMAIN}" | grep -i "strict-transport-security" || true)
if [ -n "${HSTS_HEADER}" ]; then
    echo "    HSTS Presente: ${HSTS_HEADER}"
else
    echo "    AVISO: Cabeçalho HSTS não detectado."
fi

echo "==> 4. Verificando terminação Cloudflare (CF-Ray)..."
CF_RAY=$(curl -s -I "https://${DOMAIN}" | grep -i "cf-ray" || true)
if [ -n "${CF_RAY}" ]; then
    echo "    Cloudflare ativo: ${CF_RAY}"
else
    echo "    AVISO: CF-Ray não encontrado no cabeçalho de resposta."
fi

echo "==> Verificação concluída."
