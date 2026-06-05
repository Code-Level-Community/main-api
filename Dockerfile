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
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1001 quarkus && adduser -u 1001 -G quarkus -s /bin/sh -D quarkus

WORKDIR /app/quarkus-app
COPY --from=builder /code/target/quarkus-app .
RUN chown -R 1001:1001 /app/quarkus-app

USER 1001

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport" \
    QUARKUS_PROFILE=dev

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec /opt/java/openjdk/bin/java $JAVA_OPTS -Dquarkus.profile=$QUARKUS_PROFILE -jar /app/quarkus-app/quarkus-run.jar"]
