## Estágio 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /code

COPY mvnw .mvn* ./
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests

## Estágio 2: Execução
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /code/target/quarkus-app /app/quarkus-app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quarkus-app/quarkus-run.jar"]
