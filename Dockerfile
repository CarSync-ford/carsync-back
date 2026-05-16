# ---- Runtime only (o build é feito pelo GitHub Actions) ----
FROM eclipse-temurin:21-jre-alpine

# Usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Application Insights Java agent (auto-instrumentation)
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.4/applicationinsights-agent-3.5.4.jar /opt/agent.jar

# Copia o JAR compilado pelo CI
COPY target/*.jar app.jar

# Permissões corretas
RUN chown spring:spring app.jar
USER spring:spring

EXPOSE 8080

# Hardening da JVM + Application Insights agent
ENTRYPOINT ["java", \
    "-javaagent:/opt/agent.jar", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+DisableAttachMechanism", \
    "-jar", "app.jar"]
