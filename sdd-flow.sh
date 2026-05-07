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

echo "🚀 3. Aider finalizado. Criando o Pull Request automaticamente..."
# Usa o GitHub CLI para abrir o PR sem te perguntar nada
gh pr create \
  --title "feat: $BRANCH_NAME" \
  --body "Implementação automatizada baseada na Spec: $SPEC_FILE" \
  --head "$BRANCH_NAME" \
  --base main

echo "✅ Sucesso! O Pull Request foi aberto."
echo "👀 Revise o PR aqui: $(gh pr view --json url -q .url)"
