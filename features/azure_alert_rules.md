# Azure Monitor Alert Rules Setup

Step-by-step guide to configure Azure Monitor alerts for the CarSync backend application.

## Prerequisites

- Application deployed to Azure App Service with Application Insights enabled
- Logs flowing to a Log Analytics workspace (via `traces` table)

---

## 1. Create Action Group

1. Azure Portal → **Monitor** → **Alerts** → **Action groups** → **+ Create**
2. **Basics:**
   - Subscription: your subscription
   - Resource group: `rg-carsync`
   - Action group name: `ag-carsync-email`
   - Display name: `CarSync Email`
3. **Notifications:**
   - Type: **Email/SMS/Push/Voice**
   - Name: `email-enzo`
   - Email: `enzoreal100@gmail.com`
4. Click **Review + create** → **Create**

---

## 2. Alert Rules

For each rule below:

1. Azure Portal → **Monitor** → **Alerts** → **+ Create** → **Alert rule**
2. **Scope:** select the Log Analytics workspace linked to Application Insights
3. **Condition:** Custom log search (KQL)
4. Configure the KQL query, threshold, and evaluation window as specified
5. **Actions:** select `ag-carsync-email`
6. **Details:** set name, severity, and description
7. **Review + create**

---

### 2.1 Auth Failures

| Field | Value |
|---|---|
| Rule name | `alert-auth-failures` |
| Severity | Sev 2 (Warning) |
| KQL | See below |
| Threshold | Greater than 5 |
| Evaluation window | 5 minutes |
| Frequency | 5 minutes |

```kql
traces
| where message has "AUTH_FAILURE"
| where timestamp > ago(5m)
| count
```

---

### 2.2 Invalid JWT Tokens

| Field | Value |
|---|---|
| Rule name | `alert-jwt-invalid` |
| Severity | Sev 2 (Warning) |
| KQL | See below |
| Threshold | Greater than 5 |
| Evaluation window | 5 minutes |
| Frequency | 5 minutes |

```kql
traces
| where message has "JWT_INVALID"
| where timestamp > ago(5m)
| count
```

---

### 2.3 Rate Limit Exceeded

| Field | Value |
|---|---|
| Rule name | `alert-rate-limit` |
| Severity | Sev 3 (Informational) |
| KQL | See below |
| Threshold | Greater than 10 |
| Evaluation window | 5 minutes |
| Frequency | 5 minutes |

```kql
traces
| where message has "RATE_LIMIT_EXCEEDED"
| where timestamp > ago(5m)
| count
```

---

### 2.4 Server Errors (500)

| Field | Value |
|---|---|
| Rule name | `alert-internal-errors` |
| Severity | Sev 1 (Error) |
| KQL | See below |
| Threshold | Greater than 3 |
| Evaluation window | 5 minutes |
| Frequency | 5 minutes |

```kql
traces
| where message has "INTERNAL_ERROR"
| where timestamp > ago(5m)
| count
```

---

### 2.5 Duplicate Registration Attempts

| Field | Value |
|---|---|
| Rule name | `alert-duplicate-registration` |
| Severity | Sev 3 (Informational) |
| KQL | See below |
| Threshold | Greater than 5 |
| Evaluation window | 5 minutes |
| Frequency | 5 minutes |

```kql
traces
| where message has "DUPLICATE_REGISTRATION"
| where timestamp > ago(5m)
| count
```

---

## 3. Verification

After setup, trigger each scenario and confirm email notifications arrive:

- **AUTH_FAILURE:** POST `/api/v1/auth/login` with wrong credentials 6+ times
- **JWT_INVALID:** Send requests with an expired/malformed Bearer token 6+ times
- **RATE_LIMIT_EXCEEDED:** Exceed 10 requests/second from a single IP 11+ times
- **INTERNAL_ERROR:** Trigger an unhandled exception 4+ times
- **DUPLICATE_REGISTRATION:** POST `/api/v1/users` with an existing CPF/email 6+ times
