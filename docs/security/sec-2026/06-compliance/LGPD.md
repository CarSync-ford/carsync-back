# Mapeamento e conformidade técnica — LGPD (Lei 13.709/2018)

- **Data da análise:** 2026-09-26
- **Escopo:** Tratamento de dados pessoais, telemetria veicular e dados de geolocalização no sistema CarSync.
- **Aviso legal:** Este documento reflete a análise técnica e arquitetural dos controles implementados no software. Não constitui parecer jurídico formal.

---

## 1. Inventário de dados pessoais, telemetria e geolocalização

| Categoria de dado | Campos específicos no modelo | Origem / Coleta | Classificação LGPD | Finalidade do tratamento | Base Legal proposta (Art. 7º LGPD) |
|---|---|---|---|---|---|
| **Identificação Pessoal** | Nome, CPF, Email, Telefone | Cadastro do cliente via formulário web/mobile ou integração CRM (`Customer`, `User`, `Lead`). | Dado pessoal comum | Identificação do proprietário, autenticação no sistema e comunicação sobre recall/revisão. | **Execução de contrato** (Art. 7º, V) e **Consentimento** (Art. 7º, I). |
| **Credenciais de Acesso** | Senha (hash BCrypt), segredo TOTP MFA, refresh token | Endpoint `/api/v1/auth` e rotas de segurança. | Dado pessoal de controle de acesso | Garantia da segurança da informação e prevenção a fraudes. | **Cumprimento de obrigação legal/regulatória** (Art. 7º, II) e **Legítimo interesse** (Art. 7º, IX). |
| **Dados do Veículo** | Chassi (VIN), Placa, Modelo, Ano, Quilometragem, Status de garantia | Cadastro e telemetria veicular (`Vehicle`). | Dado pessoal indireto (vinculável ao proprietário) | Acompanhamento do ciclo de vida do veículo e cálculo de risco de churn. | **Execução de contrato** (Art. 7º, V). |
| **Telemetria Operacional** | Nível de combustível, status de bateria, código de falha (DTC), histórico de O.S. | Módulos telemáticos veiculares e ordens de serviço (`ServiceOrder`). | Dado operacional / pessoal por associação | Manutenção preventiva, alertas de estoque de peças e diagnóstico preditivo. | **Execução de contrato** (Art. 7º, V) e **Legítimo interesse** (Art. 7º, IX). |
| **Geolocalização / Posição** | Coordenadas GPS (latitude, longitude), timestamps de deslocamento | Dispositivos telemáticos / app móvel durante condução. | Dado pessoal sensível por inferência de hábitos | Assistência em emergências, agendamento em concessionária próxima e logística de socorro. | **Consentimento específico e destacado** (Art. 7º, I / Art. 11) ou **Execução de contrato**. |

---

## 2. Ciclo de vida dos dados: retenção, descarte e mascaramento

### 2.1 Mascaramento em runtime (`DataMasker`) vs. Anonimização legal
O projeto implementa uma distinção fundamental exigida pela LGPD:

- **Mascaramento em tempo de execução (`DataMasker.java`):**
  - **Função:** Oculta dígitos sensíveis em logs e exibições na interface (ex: CPF formatado como `***.***.456-**`, email como `j***@carsync.me`).
  - **Natureza técnica:** É uma medida de pseudonimização/segurança para mitigar vazamento em telas ou relatórios operacionais. **Não constitui anonimização sob o Art. 12 da LGPD**, pois o dado real permanece reversível e armazenado no banco de dados para os fins contratuais legítimos.
- **Anonimização e Expurgo Definitivo (`DataRetentionService.java`):**
  - **Função:** Rotina de ciclo de vida que localiza registros inativos além do prazo legal de retenção (ex: leads descartados há mais de 180 dias sem conversão).
  - **Ação:** Sobrescreve irreversivelmente campos identificadores com hashes pseudoaleatórios ou remove os registros via expurgo (purge), garantindo que os dados não possam mais ser associados a um indivíduo por meios razoáveis disponíveis.

### 2.2 Política de retenção implementada

| Entidade / Dado | Período de retenção ativo | Ação de término de custódia | Justificativa regulatória / de negócio |
|---|---|---|---|
| **Leads não convertidos** | 180 dias da última interação | Anonimização / Expurgo automático (`DataRetentionService`) | Esgotamento da finalidade comercial sem vínculo contratual ativo. |
| **Histórico de O.S. e Garantia** | 5 anos após término da garantia | Arquivamento / Anonimização parcial de condutores | Código de Defesa do Consumidor e prazos prescricionais civis. |
| **Logs de acesso e auditoria** | Mínimo 6 meses (Marco Civil) / 1 ano (Boas práticas) | Rotação automática no Azure Log Analytics | Art. 15 do Marco Civil da Internet (Lei 12.965/2014). |

---

## 3. Risco arquitetural do histórico de auditoria (Hibernate Envers)

O uso do Hibernate Envers (`@Audited`) garante rastreabilidade contra alterações não autorizadas, mas introduz um risco específico de conformidade com a LGPD:

1. **Risco identificado:** Quando um registro da tabela `users` ou `customers` sofre alteração ou exclusão lógica, o Envers insere uma nova linha nas tabelas de auditoria (`_aud`). Se uma senha temporária, email antigo ou dado pessoal for atualizado, a versão anterior continua persistida no banco histórico de auditoria.
2. **Mitigação técnica recomendada e aplicada:**
   - **Campos sensíveis excluídos:** Anotação `@NotAudited` em atributos como senhas, segredos TOTP e tokens temporários, evitando que credenciais entrem na tabela histórica.
   - **Procedimento para Direito ao Esquecimento (Art. 18 LGPD):** Quando uma solicitação formal de exclusão for exercida pelo titular, a exclusão lógica na tabela principal deve ser complementada por script específico de expurgo/anonimização das tabelas `_aud` vinculadas ao identificador do titular, ressalvadas as obrigações legais de guarda.

---

## 4. Direitos do titular dos dados (Art. 18 LGPD)

| Direito do titular | Mecanismo técnico no CarSync |
|---|---|
| **Confirmação e Acesso** | Endpoint `/api/v1/customers/{id}/360` consolidando o perfil do cliente, seus veículos e histórico de interações. |
| **Correção de dados incompletos** | Endpoints de atualização cadastral com validação server-side. |
| **Anonimização ou Bloqueio** | Execução de rotinas do `DataRetentionService` por solicitação de encerramento de conta. |
| **Portabilidade dos dados** | Disponibilização de payloads em formato JSON e XML estruturados via negociação de conteúdo HTTP (`Accept`). |
| **Eliminação dos dados pessoais** | Rotina de expurgo com exclusão de dados operacionais e anonimização de métricas analíticas agregadas. |
