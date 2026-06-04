## Estágio 1: Build (Compilação Nativa com GraalVM)
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-java21 AS builder
USER quarkus
WORKDIR /code

# Copia os arquivos de configuração do Maven para cache de dependências
COPY --chown=quarkus:quarkus mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/pom.xml
RUN ./mvnw dependency:go-offline

# Copia o código-fonte e executa o build nativo do Quarkus
COPY --chown=quarkus:quarkus src /code/src
RUN ./mvnw package -Dnative -Dquarkus.native.native-image-xmx=3g

## Estágio 2: Execução (Baseado no seu Dockerfile.native-micro)
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

# Busca o executável nativo gerado no estágio anterior
COPY --from=builder --chown=1001:root --chmod=0755 /code/target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
