# Documentação de Segurança e Infraestrutura - carsync-api-dev (ACA)

## Criptografia em Repouso
Durante o provisionamento do servidor Azure Database for PostgreSQL para a API, a criptografia de dados em repouso (Data at Rest) foi validada e mantida habilitada por padrão utilizando as chaves de criptografia gerenciadas pela Microsoft, protegendo todos os volumes de dados armazenados pelo sistema.

## Forçar HTTPS e Término TLS
Nas configurações de Ingress do aplicativo `carsync-api-dev` no Azure Container Apps, a opção "Insecure Traffic" foi desativada para garantir o redirecionamento automático de requisições HTTP para HTTPS, centralizando o término seguro do certificado (focado em TLS 1.2+) diretamente na borda do contêiner.

## CORS e Proteção contra Abusos (Rate Limiting)
As políticas de CORS foram restritas nativamente através das configurações do Ingress do ACA, liberando apenas o domínio específico do frontend. Para a proteção contra picos de requisições, preparou-se o terreno para a atuação do Azure API Management (APIM) à frente do tráfego, limitando abusos por IP.

## Telemetria e Monitoramento de Saúde
A aplicação no ACA foi vinculada a um Azure Log Analytics Workspace, e as sondas de saúde (*Liveness* e *Readiness Probes*) foram devidamente configuradas para consultar os endpoints do Spring Boot Actuator, permitindo que a infraestrutura monitore ativamente o contêiner e o reinicie em caso de falhas.

## Centralização de Logs Estruturados
Toda a saída gerada pela aplicação Spring Boot no formato JSON é capturada nativamente pelo Container Apps e enviada para o Log Analytics Workspace, onde fica centralizada na tabela `ContainerAppConsoleLogs_CL` para viabilizar consultas avançadas de auditoria e governança.

## Gerenciamento Seguro de Segredos
Para garantir a segurança do ambiente, chaves sensíveis como *secrets* do JWT, HMAC e credenciais de banco de dados foram configuradas diretamente nos Secrets do Azure Container Apps (ACA) e GitHub Actions Secrets. Elas são injetadas diretamente como variáveis de ambiente no contêiner do `carsync-api-dev` durante o processo de deploy automatizado via GitHub Actions.

## Dimensionamento para Auditoria
O ambiente do PostgreSQL foi planejado com recursos de armazenamento e performance (*Compute/Storage*) adequados para suportar o crescimento das tabelas de histórico automático (`_AUD`), garantindo que o banco não sofra gargalos conforme o Hibernate Envers registra as alterações de auditoria.

## Monitoramento de Anomalias e Alertas
Foram arquitetadas regras de alerta no Azure Monitor utilizando consultas KQL (Kusto Query Language) no Log Analytics. Essas regras monitoram os logs de segurança emitidos pelo Spring Boot e disparam notificações caso identifiquem padrões anômalos, como múltiplas falhas de autenticação sucessivas.