# Spec: Limitação de Payload

**Data:** 18/05/2026  
**Autor:** Enzo  
**Branch:** `feat/payload-limit`

---
IMPORTANTE: Após cada uma das tasks feitas, faça um commit na branch com uma mensagem simples seguindo o exemplo: 
"feat: one-line message explaining what was made"
## 1. Objetivo

Configurar limites de tamanho de payload via `spring.servlet.multipart.*` no `application.yml` para restringir uploads a 1MB, preparando o projeto para funcionalidades futuras de upload e garantindo segurança contra payloads excessivos.

---

## 2. Alterações no Banco de Dados

Sem alterações no schema.

---

## 3. Contrato da API

Não há novos endpoints. A configuração afeta todos os endpoints que recebem multipart/form-data.

**Comportamento adicionado:**

| Cenário | Status | Descrição |
|---------|--------|-----------|
| Payload > 1MB | 413 | Payload Too Large - retorna `ApiErrorResponse` |

---

## 4. DTOs

Sem novos DTOs. A resposta de erro usa o `ApiErrorResponse` existente.

---

## 5. Regras de Negócio

1. Arquivos individuais não podem exceder 1MB (`max-file-size: 1MB`)
2. O tamanho total da requisição não pode exceder 1MB (`max-request-size: 1MB`)
3. Quando o limite é excedido, retornar HTTP 413 com mensagem: "O tamanho do arquivo excede o limite permitido de 1MB."

**Exceções esperadas:**
- `MaxUploadSizeExceededException` quando o payload excede o limite configurado

---

## 6. Testes

- [ ] Teste que verifica resposta 413 ao enviar arquivo maior que 1MB
- [ ] Verificar que o corpo da resposta segue o formato `ApiErrorResponse`

---

## 7. Critérios de Aceite

- [ ] Propriedades `spring.servlet.multipart.*` configuradas no `application.yml`
- [ ] Erros de payload retornam no formato `ApiErrorResponse` com status 413
- [ ] `mvn clean test` passa sem falhas

---

## 8. Tasks de Implementação

- [ ] **Task 1:** Adicionar configuração `spring.servlet.multipart` no `src/main/resources/application.yml` com `enabled: true`, `max-file-size: 1MB`, `max-request-size: 1MB`
- [ ] **Task 2:** Adicionar `@ExceptionHandler(MaxUploadSizeExceededException.class)` no `GlobalExceptionHandler` retornando `HttpStatus.PAYLOAD_TOO_LARGE`
- [ ] **Task 3:** Criar teste de integração validando resposta 413 ao enviar arquivo > 1MB
- [ ] **Task 4:** Rodar `mvn clean test` e garantir que todos os testes passam
