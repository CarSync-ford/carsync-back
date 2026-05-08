#!/bin/bash

BRANCH_NAME=$1
SPEC_FILE=$2

# Validação de argumentos
if [ -z "$BRANCH_NAME" ] || [ -z "$SPEC_FILE" ]; then
  echo "⚠️  Uso correto: ./scripts/sdd-flow.sh <nome-da-branch> <caminho-da-spec>"
  echo "💡 Exemplo: ./scripts/sdd-flow.sh feat/create-user features/spec-create-user.md"
  exit 1
fi

if [ ! -f "$SPEC_FILE" ]; then
  echo "❌ Erro: O arquivo de especificação '$SPEC_FILE' não foi encontrado."
  exit 1
fi

echo "🚀 Iniciando fluxo SDD..."
echo "📦 1. Atualizando main e criando a branch: $BRANCH_NAME"
git checkout main
git pull origin main
git checkout -b "$BRANCH_NAME"

echo "🧠 2. Chamando o Aider para Planejamento (Modo Architect)..."
aider --architect --message "Atue sob Spec Driven Development. Leia a especificação a seguir. 
Se algo estiver ambíguo ou exigir decisões arquiteturais, me pergunte antes de gerar o plano. 
Se estiver tudo claro, gere o plano de ação.
AQUI ESTÁ A SPEC:
$(cat "$SPEC_FILE")"

# --- O Aider roda aqui, você aprova, ele coda e o git hook faz o push ---

echo "📝 3. Implementação concluída. Extraindo dados do Git para o PR..."
# Puxa a diferença real da branch atual contra a main (ou develop, ajuste se necessário)
TARGET_BRANCH="main"
GIT_LOG=$(git log ${TARGET_BRANCH}..HEAD --pretty=format:"%h - %s" --reverse)
GIT_DIFF=$(git diff --name-status ${TARGET_BRANCH}..HEAD)

echo "🤖 4. Gerando a descrição formatada do PR com IA..."
# O Aider lê o prompt da pasta .github e formata o PR
aider --yes --message "Use as instruções do arquivo .github/PullRequestSummarizer.md para criar um arquivo chamado 'pr_description.md'. 
NÃO invente dados. Use ESTRITAMENTE o histórico abaixo:

[GIT LOG]
$GIT_LOG

[GIT DIFF STATUS]
$GIT_DIFF"

if [ ! -f "pr_description.md" ]; then
  echo "❌ Erro: O Aider falhou ao gerar o arquivo pr_description.md."
  exit 1
fi

echo "🚀 5. Abrindo o Pull Request no GitHub..."
gh pr create \
  --title "feat: $BRANCH_NAME" \
  --body-file pr_description.md \
  --head "$BRANCH_NAME" \
  --base "$TARGET_BRANCH"

# Limpeza
rm pr_description.md

echo "✅ Sucesso! O Pull Request foi aberto automatizado com a sua descrição."