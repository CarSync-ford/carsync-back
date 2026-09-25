# ---- Runtime only (o build é feito pelo GitHub Actions) ----
FROM eclipse-temurin:21-jre-alpine-3.24@sha256:1a29e1fe337eb28b5bec30f0ee8ed29f0ff80ab6f75dcf9313efe82911065a52

# Patch OS vulnerability libexpat
RUN apk add --no-cache --upgrade 'libexpat>=2.8.5-r0'

# Usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Application Insights Java agent 3.7.10 with sha256 validation
# ponytail: pinned static agent release; migrate to multi-stage fetch or internal artifact repo when corporate proxy required
RUN wget -q -O /opt/agent.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.10/applicationinsights-agent-3.7.10.jar && \
    echo "93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec  /opt/agent.jar" | sha256sum -c -

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
