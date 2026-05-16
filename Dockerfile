# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia as camadas extraídas (melhor cache de layers do Docker)
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

# Remove permissões desnecessárias
RUN chown -R appuser:appgroup /app

# Executa como usuário não-root
USER appuser

EXPOSE 8080

# Hardening da JVM: desabilita attach, limita metaspace
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+DisableAttachMechanism", \
    "-XX:MaxMetaspaceSize=128m", \
    "org.springframework.boot.loader.launch.JarLauncher"]
