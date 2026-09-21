#!/usr/bin/env bash
set -euo pipefail

# Script para restringir o Ingress do Azure Container Apps (ACA)
# permitindo tráfego EXCLUSIVAMENTE originado pelo Azure API Management (APIM)

RESOURCE_GROUP="${1:-rg-carsync-prod}"
CONTAINER_APP_NAME="${2:-aca-carsync-backend}"
APIM_NAME="${3:-apim-carsync}"

echo "==> Buscando IP público / VIP do APIM: ${APIM_NAME}..."
APIM_IP=$(az apim show \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${APIM_NAME}" \
    --query "publicIpAddresses[0]" -o tsv)

if [ -z "${APIM_IP}" ] || [ "${APIM_IP}" == "null" ]; then
    echo "ERRO: Não foi possível obter o IP público do APIM ${APIM_NAME}"
    exit 1
fi

echo "==> APIM IP identificado: ${APIM_IP}"

echo "==> Configurando IP restriction no Ingress do ACA ${CONTAINER_APP_NAME}..."
# 1. Permite o IP do APIM com prioridade alta
az containerapp ingress ip-restriction add \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${CONTAINER_APP_NAME}" \
    --rule-name "Allow-APIM-Only" \
    --ip-address-range "${APIM_IP}/32" \
    --action Allow

echo "==> Desabilitando RateLimitFilter interno no ACA (offloaded para o APIM)..."
az containerapp update \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${CONTAINER_APP_NAME}" \
    --set-env-vars RATE_LIMIT_ENABLED=false

echo "==> Ingress configurado com sucesso! ACA aceita tráfego somente via APIM (${APIM_IP})."
