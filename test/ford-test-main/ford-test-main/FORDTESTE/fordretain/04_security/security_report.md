# FordRetain — Relatório de Segurança

## 1. Autenticação e Autorização

- JWT com expiração de 60 minutos e assinatura HS256
- RBAC: perfil `consultor` (acesso à concessionária própria) e `gestor` (acesso total)
- Tokens não armazenados em `localStorage` — uso de `AsyncStorage` no mobile
- Em produção: substituir mock por Active Directory Ford

## 2. Validação de Entradas

| Campo | Regra |
|-------|-------|
| `vin_hash` | Apenas alfanumérico, 10–64 caracteres |
| `dealer_code` | Inteiro positivo < 100.000 |
| Campos de texto | Sanitização contra XSS (remove `<>"';\\`) |
| Erros | Sem exposição de stack trace em produção |

## 3. Proteção da API

- **HTTPS/TLS 1.2+** obrigatório em produção
- **Rate limiting:** 100 req/min por IP (geral) · 10 req/min por IP (auth)
- **CORS** configurado para domínios autorizados em produção
- **Headers de segurança:** `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`

## 4. Privacidade dos Dados (LGPD)

- VINs armazenados como hash SHA-256 — sem exposição de placa ou dados pessoais
- Dados de clientes anonimizados nos modelos de ML
- Logs de auditoria sem dados sensíveis (truncamento do recurso após 20 chars)
- Sem coleta de localização sem consentimento explícito

## 5. Monitoramento e Auditoria

- Logs estruturados em JSON para cada acesso a recurso sensível
- Rastreabilidade por usuário em ações críticas (score, risco)
- Recomendado: integração com SIEM da Ford em produção

## 6. Dependências

```bash
# Verificar vulnerabilidades conhecidas
pip-audit -r requirements.txt
npm audit --audit-level=moderate
```

## 7. Checklist de Produção

- [ ] Revogar `SECRET_KEY` de desenvolvimento
- [ ] Configurar CORS apenas para domínios Ford
- [ ] Ativar HTTPS com certificado válido
- [ ] Substituir mock de usuários por AD Ford
- [ ] Configurar alertas de tentativas de brute-force
- [ ] Implementar rotação de tokens (refresh token)
