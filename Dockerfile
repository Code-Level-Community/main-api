## Estágio 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /code

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests

## Estágio 2: Execução
FROM registry.access.redhat.com/ubi9/ubi-minimal:latest
RUN microdnf install -y java-21-openjdk-headless && microdnf clean all

RUN groupadd -g 1001 quarkus && useradd -u 1001 -g quarkus quarkus

WORKDIR /app/quarkus-app
COPY --from=builder /code/target/quarkus-app .
RUN chown -R 1001:1001 /app/quarkus-app

USER 1001

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dquarkus.profile=dev -jar quarkus-run.jar"]
