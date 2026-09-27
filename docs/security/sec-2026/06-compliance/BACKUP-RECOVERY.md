# Rotina de backup e recuperação de desastres (PostgreSQL)

- **Data da publicação:** 2026-09-26
- **Tecnologia do banco de dados:** PostgreSQL (driver `org.postgresql:postgresql` em produção; schema versionado via Flyway `db/migration/V1__create_schema.sql` e auditoria Hibernate Envers).
- **Ambiente:** Azure Database for PostgreSQL / Contêiner de banco provisionado em rede isolada.

---

## 1. Inventário de dados e estratégia de backup

| Camada de persistência | Conteúdo armazenado | Criticidade | Estratégia de backup primária |
|---|---|---|---|
| **Tabelas Operacionais** | Clientes, Veículos, Leads, Ordens de Serviço, Estoque | Alta | Snapshots diários automáticos + log de transações WAL contínuo (PITR). |
| **Tabelas de Auditoria** | Histórico Envers (`_aud`) e revisões (`revinfo`) | Alta | Backup integrado na mesma transação/snapshot do banco relacional. |
| **Metadados de Migração** | Tabela `flyway_schema_history` | Crítica | Versionamento de código no Git + backup relacional integrado. |

### 1.1 Políticas e retenção de cópias
- **Point-In-Time Restore (PITR):** Em ambiente gerenciado (Azure Database for PostgreSQL Flexible Server), arquivamento contínuo de Write-Ahead Logging (WAL) permitindo restauração para qualquer segundo nos últimos **35 dias**.
- **Backup Lógico Completo (`pg_dump`):** Execução agendada diária às 02:00 UTC gerando arquivo compactado customizado (`-Fc`):
  ```bash
  pg_dump -h $DB_HOST -p 5432 -U $DB_USER -d carsync -Fc -b -v -f /backups/carsync_$(date +%Y%m%d_%H%M%S).dump
  ```
- **Retenção de backups lógicos:**
  - Backups diários retidos por 30 dias.
  - Backups mensais (primeiro dia de cada mês) retidos por 12 meses em storage redundante com criptografia em repouso AES-256.

### 1.2 Objetivos de recuperação (Metas propostas)
- **RPO (Recovery Point Objective):** <= 5 minutos em recuperação via WAL / PITR; máximo de 24 horas em recuperação puramente lógica.
- **RTO (Recovery Time Objective):** <= 2 horas para disponibilização completa do banco em nova instância isolada.

---

## 2. Procedimento de restauração em ambiente isolado (Passo a passo)

> **REGRA DE SEGURANÇA MANDATÓRIA:** É expressamente proibido executar procedimentos de restauração diretamente sobre o banco de produção ativo sem autorização expressa e janela de manutenção formalizada. Testes de restauração devem ser conduzidos exclusivamente em instâncias temporárias isoladas (staging/test).

### Etapa 1: Provisionamento de instância de teste
Criar contêiner ou instância de banco isolada sem tráfego de produção:
```bash
docker run --name pg-restore-test -e POSTGRES_DB=carsync_test -e POSTGRES_PASSWORD=RestoreSecPass2026! -p 5433:5432 -d postgres:16-alpine
```

### Etapa 2: Execução da restauração do backup
Restaurar o dump lógico no banco isolado:
```bash
pg_restore -h localhost -p 5433 -U postgres -d carsync_test -v /backups/carsync_snapshot.dump
```

### Etapa 3: Checklist de validação e integridade
Após a carga dos dados, executar as seguintes validações:
1. **Consistência de Migrações Flyway:**
   ```sql
   SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```
   *Critério de aceitação:* Todas as migrações com `success = true`.
2. **Contagem de registros e tabelas críticas:**
   ```sql
   SELECT count(*) FROM users;
   SELECT count(*) FROM vehicles;
   SELECT count(*) FROM customers;
   SELECT count(*) FROM revinfo;
   ```
   *Critério de aceitação:* Contagens equivalentes ao momento do snapshot.
3. **Teste de integridade referencial:** Executar queries de junção entre veículos, clientes e interações telemáticas.
4. **Desconexão e Descarte do Ambiente de Teste:** Finalizada a validação, a instância temporária `pg-restore-test` é destruída, garantindo que cópias de dados em homologação não permaneçam expostas.

---

## 3. Estado de verificação desta rotina

- **Procedimento documental:** Homologado e versionado.
- **Status de execução real em produção:** Pendente de autorização de janela de infraestrutura. A arquitetura de banco de dados e a configuração do driver PostgreSQL foram testadas e validadas em ambiente local e CI.
