# ADR-011: Compilação Native com GraalVM

## Status
Accepted

## Context

Aplicações JVM tradicionais têm startup lento (segundos) e alto consumo de memória em repouso (~200-400MB). Para ambientes containerizados com escalonamento horizontal e serverless, isso gera latência na inicialização de novas réplicas e custo de infraestrutura elevado. O Quarkus foi projetado desde o início com native image como target primário.

## Decision

Adotar **GraalVM Native Image** como target de build para produção via Maven profile `native`:

```bash
# Build native
./mvnw package -Dnative

# Execução
./target/demo-quarkus-graalvm-1.0.0-SNAPSHOT-runner
```

### Restrições obrigatórias para compatibilidade native

1. **Sem reflexão dinâmica** sem registro explícito — usar CDI/ARC para injeção.
2. **Panache** é compatível com native por design — queries geradas em compile-time.
3. **SmallRye JWT, Hibernate Validator, Jackson** têm suporte native no Quarkus sem configuração adicional.
4. Classes com reflexão necessária devem ser registradas em `src/main/resources/META-INF/native-image/reflect-config.json`.

### Imagem Docker
```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal
COPY target/*-runner /work/application
CMD ["/work/application", "-Dquarkus.http.host=0.0.0.0"]
```

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| JVM tradicional em container | Startup lento (~3-5s), ~200MB de memória em repouso, imagem Docker maior |
| JVM com JIT aquecido (long-running) | Melhor throughput em pico, mas incompatível com escalonamento horizontal rápido |

## Motivation

Startup em menos de 50ms e ~40MB de memória em repouso — viabiliza escalonamento horizontal rápido e reduz custo em ambientes pay-per-use.

## Consequences

### Positive
- Startup em menos de 50ms (vs ~3-5 segundos na JVM).
- Consumo de memória em repouso ~40MB (vs ~200MB na JVM).
- Imagem Docker menor: ~100MB vs ~400MB com JVM.
- Escalabilidade horizontal mais rápida: novas réplicas ficam prontas em segundos.
- Menor custo de infraestrutura em ambientes pay-per-use (serverless, spot instances).

### Negative / Trade-offs
- Build extremamente lento: **10-15 minutos** para compilar o native image.
- Debugging de native image é mais complexo — stack traces podem ser menos detalhados.
- Reflexão dinâmica é proibida sem registro — bibliotecas externas podem exigir configuração manual.
- Peak throughput ligeiramente inferior ao JVM com JIT aquecido.
- Profiling e ferramentas de observabilidade JVM (JFR, JVisualVM) não funcionam em native.
