# ADR 001: Decisão Arquitetural - In-Memory Rate Limiting vs Rate Limiting Distribuído

## Status
Aceito (com estratégia de offloading na borda via APIM/Cloudflare)

## Data
2026-09-18

## Contexto
A aplicação CarSync (Spring Boot 3.3.2, Java 21) é implantada no Azure Container Apps (ACA) com proteção de borda Cloudflare. Para mitigar ataques de força bruta, DoS e consumo abusivo de recursos (OWASP API4:2023 - Unrestricted Resource Consumption), implementou-se o `RateLimitFilter` utilizando a biblioteca Bucket4j (token bucket algorithm), limitando requisições por endereço IP (`CF-Connecting-IP` / `X-Forwarded-For`).

No modelo atual de implantação com réplica única (`minReplicas: 1`, `maxReplicas: 1`), o rate limiting in-memory baseado em `ConcurrentHashMap<String, Bucket>` atende perfeitamente ao requisito com latência sub-milissegundo e custo adicional de infraestrutura zero.

Entretanto, em cenários de escalabilidade horizontal com $N$ réplicas ativas no Azure Container Apps, cada contêiner mantém seu próprio estado de buckets em memória. O limite efetivo por IP passa a ser $N \times 10$ requisições por segundo, gerando divergência entre instâncias.

## Alternativas Avaliadas

### 1. In-Memory Rate Limiting Local (Bucket4j Core) - *Opção Atual*
- **Vantagens:**
  - Latência zero de rede (execução direta na JVM).
  - Custo zero de infraestrutura adicional (dispensa cluster Redis dedicado).
  - Sem ponto único de falha externo para validação de requisições.
  - Simplicidade operacional máxima para desenvolvimento local e pipelines CI.
- **Desvantagens:**
  - Limite rate limit não sincronizado entre múltiplas réplicas do contêiner.
  - Consumo de memória na JVM proporcional ao número de IPs simultâneos (mitigado por expiração natural de tokens).

### 2. Rate Limiting Distribuído com Redis (Bucket4j + Redis/Lettuce)
- **Vantagens:**
  - Sincronização atômica rigorosa entre todas as réplicas do backend.
  - Limite global estritamente respeitado em qualquer escala horizontal.
- **Desvantagens:**
  - Custo financeiro significativo de provisionamento do Azure Cache for Redis (SKU Standard/Premium).
  - Adiciona latência de rede (1-3ms) em cada requisição HTTP recebida.
  - Introduz dependência crítica externa: se o Redis degradar ou falhar, a aplicação precisa de fallback complexo ou bloqueia tráfego legítimo.
  - Complexidade de pool de conexões e overhead de serialização.

### 3. Rate Limiting na Borda via Azure API Management (APIM) e Cloudflare - *Estratégia Recomendada para Multi-Réplica*
- **Vantagens:**
  - Intercepta e descarta requisições maliciosas ou abusivas na borda antes de consumirem CPU, memória, rede ou conexões de banco de dados do ACA.
  - Unifica rate limiting, cotas por subscription, validação JWT de borda e mitigação de DDoS.
  - O backend ACA pode desativar o filtro interno (`RATE_LIMIT_ENABLED=false`), liberando capacidade computacional dos contêineres.
  - Alinhado à arquitetura de referência corporativa Microsoft Azure / Cloudflare.
- **Desvantagens:**
  - Exige provisionamento e configuração de policies no Azure API Management.
  - Necessita restrição de ingress no ACA para aceitar tráfego apenas do APIM.

## Decisão
1. **Ambiente Atual e Baseline (Single-Replica / Dev / Test):**
   - Manter `RateLimitFilter` in-memory com Bucket4j core.
   - Parametrizar capacidade (`rate-limit.capacity`), taxa (`rate-limit.tokens-per-second`) e ativação (`rate-limit.enabled: ${RATE_LIMIT_ENABLED:true}`) no `application.yml`.

2. **Escala Horizontal Multi-Réplica (Produção):**
   - **Caminho Principal:** Delegar o rate limiting para a borda no **Azure API Management (APIM)** e Cloudflare.
   - Ao ativar as policies de rate limiting no APIM, desativar o filtro in-memory no backend configurando a variável de ambiente `RATE_LIMIT_ENABLED=false` no Azure Container Apps, evitando duplicação de processamento.
   - **Caminho Alternativo (se APIM não for utilizado):** Migrar para Redis-backed Bucket4j utilizando o starter `bucket4j-redis` integrado ao Redisson/Lettuce, conforme especificação de migração abaixo.

## Guia de Migração para Redis (Caso Requerido Sem APIM)

Se for necessário rate limiting distribuído a nível de aplicação sem APIM:

1. **Dependências (`pom.xml`):**
   ```xml
   <dependency>
       <groupId>com.bucket4j</groupId>
       <artifactId>bucket4j-redis</artifactId>
       <version>8.10.1</version>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```

2. **Configuração (`application.yml`):**
   ```yaml
   spring:
     data:
       redis:
         host: ${REDIS_HOST:localhost}
         port: ${REDIS_PORT:6379}
         password: ${REDIS_PASSWORD:}
         ssl:
           enabled: ${REDIS_SSL:true}
   rate-limit:
     distributed:
       enabled: true
   ```

3. **Proxy Manager Bean:**
   ```java
   @Bean
   public ProxyManager<String> lettuceProxyManager(RedisClient redisClient) {
       return LettuceBasedProxyManager.builderFor(redisClient)
               .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
               .build();
   }
   ```

## Consequências
- **Positivas:**
  - Zero custo financeiro e operacional com cache distribuído durante as fases iniciais.
  - Transição transparente: desativação por variável de ambiente sem alteração de código quando o APIM for ativado.
  - Código desacoplado e aderente aos princípios YAGNI (You Aren't Gonna Need It).
- **Negativas/Limitações:**
  - Até que o APIM seja provisionado ou o Redis seja configurado, cada réplica adicional do ACA aumenta o throughput tolerado por IP proporcionalmente ao número de réplicas.
