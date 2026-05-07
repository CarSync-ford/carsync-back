# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime stage (PRODUÇÃO) ----
# Usando JRE para ficar mais leve e seguro
FROM eclipse-temurin:21-jre 
WORKDIR /app

# Copia o jar do build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]