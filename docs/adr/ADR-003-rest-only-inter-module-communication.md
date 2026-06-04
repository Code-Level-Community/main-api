# ADR-003: Comunicação entre Módulos Exclusivamente via REST

## Status
Accepted

## Context

Em um monólito modular, é tecnicamente possível injetar um serviço de qualquer módulo em qualquer outro via CDI (`@Inject`). Isso cria acoplamento forte entre módulos, risco de dependências circulares e torna impossível testar ou extrair módulos de forma independente.

## Decision

Módulos comunicam-se **exclusivamente via HTTP REST**:

1. O módulo produtor expõe um endpoint público em `http/rest/routes/*Resource.java`.
2. O módulo consumidor declara um REST client com `@RegisterRestClient`.
3. O módulo consumidor mapeia a resposta para seus próprios DTOs internos.

```java
// PROIBIDO: injeção direta cross-módulo
@Inject
private CourseService courseService;

// CORRETO: REST client com DTO próprio
@RegisterRestClient(baseUri = "http://localhost:8080/api")
public interface CourseClient {
    @GET
    @Path("/courses/{id}")
    CoursePublicDto getCourse(@PathParam("id") UUID courseId);
}
```

DTOs compartilhados entre módulos residem em `com.codelevel.shared.contract.dto` para evitar dependências diretas entre pacotes de módulo.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Injeção direta via CDI cross-módulo | Acoplamento forte, dependências circulares, impossível testar ou extrair módulos independentemente |
| Shared service layer | Cria um "módulo zero" que todos dependem, quebrando o isolamento |

## Motivation

Fronteiras de módulo explícitas e verificáveis sem enforcement de compilador. A `baseUri` do REST client é o único ponto a alterar para extrair um módulo como microsserviço.

## Consequences

### Positive
- Fronteiras de módulo explícitas e verificáveis: basta procurar `@RegisterRestClient`.
- Cada módulo pode ser extraído como microsserviço apenas trocando a `baseUri` do client.
- Testes de integração por módulo são possíveis sem inicializar módulos vizinhos.
- Evita dependências circulares em tempo de compilação.
- Contratos de API explícitos facilitam versionamento e evolução independente.

### Negative / Trade-offs
- Overhead de serialização/desserialização JSON mesmo sendo chamadas same-JVM.
- Falhas em módulos vizinhos se propagam via HTTP (timeouts, 404, 500) em vez de exceções Java diretas.
- Mais código boilerplate: interface de client, DTO de resposta, tratamento de erro HTTP.
- Transações distribuídas impossíveis entre módulos; precisa de padrões como Saga se consistência eventual não for aceitável.