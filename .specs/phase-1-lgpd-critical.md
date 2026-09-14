# Phase 1 - LGPD Crítico (Semanas 1-2)

**Prioridade:** P0 - Bloqueante | **Esforço:** ~2 semanas

Bloqueia compliance LGPD. Dois pilares independentes: Anonymization Pipeline (SEC-001) e Data Retention (SEC-002).

---

## 1.1 Anonymization Pipeline (SEC-001)

### Entregáveis

#### Migração V6: `V6__add_analyst_user_type.sql`
```sql
INSERT INTO user_type (id, name, description) VALUES (3, 'ANALYST', 'Analista com acesso a dados anonimizados');
```

#### Utilitário: `br.com.sprint1.challenge.util.DataMasker`
Métodos estáticos para mascarar:
- CPF: `123.456.789-00` → `***.***.***-**`
- Email: `user@domain.com` → `u***@domain.com`
- Phone: `(11) 99999-9999` → `(**) ****-****`
- Name: `João Silva` → `J*** S****`

#### DTOs Analytics (novos)
- `CustomerAnalyticsView` - campos mascarados
- `LeadAnalyticsView` - campos mascarados
- `VehicleAnalyticsView` - sem PII

#### Endpoints: `/api/v1/analytics/**`
- `@PreAuthorize("hasRole('ANALYST')")`
- Retornam DTOs mascarados
- Log: `INFO ANALYTICS_ACCESS user:{} resource:{}`

#### Testes
- Integração: USER recebe 403, ANALYST recebe 200
- Validação: campos PII mascarados no response

---

## 1.2 Data Retention / Secure Disposal (SEC-002)

### Entregáveis

#### Migração V7: `deleted_at` columns
```sql
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE leads ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_customers_deleted_at ON customers(deleted_at);
CREATE INDEX idx_leads_deleted_at ON leads(deleted_at);
```

#### Entidades: Add `@Column(name = "deleted_at") LocalDateTime deletedAt`
- `User.java`
- `Customer.java`
- `Lead.java`

#### Configuração: `application.yml`
```yaml
data-retention:
  hard-delete-days: 30
  anonymization-inactive-years: 5
  schedule:
    hard-delete: "0 0 2 * * ?"      # Diário 02:00
    anonymization: "0 0 3 ? * SUN"  # Domingo 03:00
```

#### `DataRetentionProperties` class
Mapeia configuração acima.

#### `DataRetentionService` com 2 Jobs `@Scheduled`
- **Job 1 (Diário 02:00):** Hard delete onde `deleted_at > 30 dias`
- **Job 2 (Semanal Domingo 03:00):** Anonimização PII usuários inativos `last_login > 5 anos`

#### Métricas Micrometer
```java
Counter dataRetentionRemoved = Counter.builder("data_retention.removed")
    .tag("type", "hard_delete|anonymization")
    .register(meterRegistry);
```

#### Testes
- Usar `Clock` injetado (`java.time.Clock`) para avançar tempo
- Verificar contadores métricas

---

## Critério de Pronto Fase 1

- [ ] Migração V6 aplicada (ANALYST role existe no banco)
- [ ] `DataMasker` com 100% cobertura unitária (todos métodos)
- [ ] Analytics DTOs mascarados funcionando
- [ ] Endpoints `/api/v1/analytics/**` retornam 403 para USER, 200 para ANALYST
- [ ] Migração V7 aplicada (soft delete columns + índices)
- [ ] `DataRetentionService` com 2 jobs executando (testados com Clock)
- [ ] Métricas `data_retention.removed` visíveis no App Insights

---

## Notas de Implementação

- **Multi-tenancy:** Não existe. Se adicionar no futuro, revisar `DataMasker` e `DataRetentionService` para incluir `dealership_id`
- **Event-driven:** Não há message broker. Jobs usam `@Scheduled` (funciona no ACA single instance). Para multi-replica: considerar **ShedLock** ou mover para **Azure Functions Timer Trigger**
- **Testes:** Perfil `test` usa H2. Migrações V6-V7 precisam versões H2 em `src/test/resources/db/migration/h2/`