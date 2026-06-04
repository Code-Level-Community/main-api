# ADR-006: Hierarquia de Exceções com Mappers HTTP

## Status
Accepted

## Context

Sem uma estratégia clara de tratamento de erros, serviços lançam `WebApplicationException` ou constroem `Response` diretamente — acoplando o domínio de negócio ao protocolo HTTP. Respostas de erro inconsistentes (diferentes formatos de JSON de erro por endpoint) dificultam o consumo da API por clientes.

## Decision

Adotar uma **hierarquia de exceções de domínio** desacoplada do HTTP, com **mappers dedicados** na camada HTTP que traduzem exceções para respostas HTTP:

### Hierarquia de Exceções (em `com.codelevel.shared.exception`)

```
RuntimeException
└── ApplicationException          ← base de todas as exceções do sistema
    ├── ResourceNotFound          ← recurso não encontrado
    ├── ResourceAlreadyExists     ← conflito de unicidade
    └── BusinessRuleException     ← regra de negócio violada

InvalidCredentials                ← autenticação falhou (módulo identity)
```

### Mappers HTTP (em cada módulo, `http/rest/handler/`)

```java
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<ResourceNotFound> {
    @Override
    public Response toResponse(ResourceNotFound ex) {
        return Response.status(404)
            .entity(new ErrorResponse(404, ex.getMessage()))
            .build();
    }
}
```

| Exception | HTTP Status |
|---|---|
| `ResourceNotFound` | 404 Not Found |
| `ResourceAlreadyExists` | 409 Conflict |
| `BusinessRuleException` | 422 Unprocessable Entity |
| `ApplicationException` | 400 Bad Request |
| `InvalidCredentials` | 401 Unauthorized |
| `Exception` (fallback) | 500 Internal Server Error |

Formato de resposta de erro padronizado: `ErrorResponse(int status, String message)`.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Lançar `WebApplicationException` diretamente nos serviços | Acopla domínio ao protocolo HTTP; serviços não podem ser reutilizados fora de contextos HTTP |
| Formato de erro por endpoint | Respostas inconsistentes; cada endpoint pode retornar JSON diferente para erros |

## Motivation

Serviços e objetos de domínio são completamente agnósticos ao protocolo HTTP. Mapeamentos de exceção → status HTTP ficam auditáveis em um único lugar (`handler/`), com formato de erro consistente em todos os endpoints.

## Consequences

### Positive
- Serviços e objetos de domínio são completamente agnósticos ao protocolo HTTP.
- Mapeamentos de exceção → status HTTP auditáveis em um único lugar (`handler/`).
- Formato de erro consistente em todos os endpoints da API.
- Adicionar um novo tipo de exceção é isolado: cria a exceção + cria o mapper.
- Testabilidade: services podem ser testados sem mockar contexto HTTP.

### Negative / Trade-offs
- Requer criação de um mapper para cada novo tipo de exceção.
- Desenvolvedores precisam conhecer a hierarquia para escolher a exceção correta.
- O mapper global (`GlobalExceptionMapper`) pode mascarar exceções inesperadas como 500 sem log adequado se não configurado corretamente.
- Exceções de validação do Hibernate Validator (`ConstraintViolationException`) requerem mapper adicional separado da hierarquia de domínio.