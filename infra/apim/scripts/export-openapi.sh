#!/usr/bin/env bash
set -euo pipefail

# Script para exportar a especificação OpenAPI 3.0 do backend CarSync para importação no APIM
BASE_URL="${1:-http://localhost:8080}"
OUTPUT_FILE="${2:-infra/apim/openapi-spec.json}"

echo "==> Exportando OpenAPI de: ${BASE_URL}/v3/api-docs"
mkdir -p "$(dirname "${OUTPUT_FILE}")"

curl -fsSL "${BASE_URL}/v3/api-docs" -o "${OUTPUT_FILE}"

echo "==> OpenAPI exportado com sucesso em: ${OUTPUT_FILE}"
echo "==> Validando integridade do JSON..."
if command -v jq >/dev/null 2>&1; then
    TITLE=$(jq -r '.info.title // empty' "${OUTPUT_FILE}")
    VERSION=$(jq -r '.info.version // empty' "${OUTPUT_FILE}")
    PATHS_COUNT=$(jq '.paths | length' "${OUTPUT_FILE}")
    echo "    Título: ${TITLE}"
    echo "    Versão: ${VERSION}"
    echo "    Endpoints mapeados: ${PATHS_COUNT}"
else
    head -n 10 "${OUTPUT_FILE}"
fi

echo "==> Pronto para importação no Azure API Management via CLI:"
echo "    az apim api import --resource-group <rg> --service-name <apim-name> \\"
echo "      --api-id carsync-api --path '/api' --specification-format OpenApiJson \\"
echo "      --specification-path '${OUTPUT_FILE}'"
