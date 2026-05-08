#!/bin/bash

# Recebe o nome da branch e o arquivo de spec
BRANCH_NAME=$1
SPEC_FILE=$2

if [ -z "$BRANCH_NAME" ] || [ -z "$SPEC_FILE" ]; then
  echo "Uso correto: ./sdd-flow.sh <nome-da-branch> <arquivo-spec.md>"
  exit 1
fi

echo "🚀 Iniciando fluxo SDD..."
echo "📦 1. Criando a branch: $BRANCH_NAME"
# Atualiza a main e cria a nova branch
git checkout main
git pull origin main
git checkout -b "$BRANCH_NAME"

echo "🧠 2. Chamando o Aider para Planejamento (Modo Architect)..."
# Injeta o conteúdo da Spec no Aider e dá a instrução de não tomar decisões
aider --architect --message "Atue sob Spec Driven Development. Leia a especificação a seguir. 
Se algo estiver ambíguo ou exigir decisões arquiteturais fora da spec, me pergunte antes de gerar o plano. 
Se estiver tudo claro, gere o plano de ação.
AQUI ESTÁ A SPEC:
$(cat $SPEC_FILE)"

# --- O SCRIPT PAUSA AQUI ---
# Neste momento:
# 1. O Aider faz perguntas (se houver).
# 2. O Aider gera o plano.
# 3. O Aider pergunta: "Allow execution of this plan? (Y/n)". Você aperta Enter.
# 4. O Aider escreve o código, commita (Conventional Commits) e o Git Hook faz o push.
# 5. Quando ele terminar e você estiver satisfeito, você digita /exit no Aider.
# ---------------------------

echo "🚀 3. Implementação concluída. Gerando a descrição do PR com IA..."

# Extrai a verdade absoluta do Git de forma limpa (comparando com a develop)
GIT_LOG=$(git log develop..HEAD --pretty=format:"%h - %s" --reverse)
GIT_DIFF=$(git diff --name-status develop..HEAD)

# Faz uma segunda chamada do Aider 100% silenciosa (--yes) apenas para gerar o arquivo
aider --yes --message "Use as instruções do arquivo .github/PullRequestSummarizer.md para criar um arquivo chamado 'pr_description.md'. 
NÃO invente dados. Use ESTRITAMENTE o histórico abaixo:

[GIT LOG]
$GIT_LOG

[GIT DIFF STATUS]
$GIT_DIFF"

echo "📦 4. Abrindo o Pull Request no GitHub..."
# O GitHub CLI consome o arquivo gerado magicamente pelo Aider
gh pr create \
  --title "feat: $BRANCH_NAME" \
  --body-file pr_description.md \
  --head "$BRANCH_NAME" \
  --base develop

# Limpa o arquivo temporário para manter o repositório limpo
rm pr_description.md

echo "✅ Sucesso! O Pull Request foi aberto com a sua descrição automatizada."
