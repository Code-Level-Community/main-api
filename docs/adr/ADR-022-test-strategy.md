# ADR-022: Estratégia de Testes — Unitário, Service IT e Resource IT

## Status
Accepted

## Context

Um projeto com múltiplos módulos, lógica de domínio rica e comunicação inter-módulo via REST precisa de uma estratégia de testes que cubra três camadas distintas sem duplicar esforço ou tornar o ciclo de feedback lento:

- **Lógica de domínio pura** (value objects, validações): não depende de infraestrutura; testar com JUnit puro é o mais rápido.
- **Lógica de serviço** (regras de negócio com banco): exige container Quarkus rodando para testar queries Panache, transações e constraints de banco.
- **Contrato HTTP** (endpoints, status codes, JSON): exige stack JAX-RS + JWT + exceção mapeada para verificar o comportamento completo da API.

## Decision

### Três tipos de teste com papéis distintos

| Type | Suffix | Framework | Quarkus Container | Database | What it validates |
|---|---|---|---|---|---|
| Unit | `*Test` | JUnit 5 | No | No | Value object invariants and domain logic |
| Service IT | `*ServiceIT` | `@QuarkusTest` + `@TestTransaction` | Yes | H2 in-memory | Business rules, queries, constraints |
| Resource IT | `*ResourceIT` | `@QuarkusTest` + REST-assured | Yes | H2 in-memory | HTTP status, JSON format, RBAC |

### Isolamento de dados entre testes

- **Service IT**: usa `@TestTransaction` para rollback automático após cada teste.
- **Resource IT**: não usa `@TestTransaction` — chamadas HTTP causam commits reais. Cada teste cria entidades com IDs únicos e faz assertions nos próprios IDs.

### Autenticação em Resource IT

Tokens JWT de teste são gerados programaticamente via `io.quarkus.test.security.TestSecurity` ou via endpoint `/auth/login` com credenciais pré-criadas no método `@BeforeAll`.

### Biblioteca HTTP

REST-assured é a escolha padrão para Resource IT. Configuração centralizada em `BaseResourceTest` com `RestAssured.baseURI` e content-type defaults.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Apenas testes unitários | Não valida queries Panache, constraints de banco ou transações |
| Apenas `@QuarkusTest` para tudo | Ciclo de feedback mais lento para validar value objects simples |
| Mockito para serviços em Resource IT | Torna os testes frágeis e desconectados do comportamento real |
| Testcontainers com PostgreSQL real | Custo de setup mais alto; H2 com modo PostgreSQL é suficiente para validar a maioria das queries |

## Motivation

- `@TestTransaction` elimina poluição de dados entre Service ITs sem necessidade de `@AfterEach` manual.
- A separação por sufixo (`*Test`, `*ServiceIT`, `*ResourceIT`) permite rodar subsets via `-Dtest="*ServiceIT"`.
- REST-assured é idiomático no ecossistema Quarkus e expressivo para validar JSON e status HTTP.

## Consequences

### Positive
- Testes de domínio são rápidos (< 1 ms por teste) e executados sem overhead de container.
- Resource ITs validam o comportamento real da API incluindo mapeamento de exceções e RBAC.
- A ausência de mocks torna os testes mais realistas e refatorações mais seguras.

### Negative / Trade-offs
- Resource ITs podem deixar dados no banco se não usarem IDs únicos — responsabilidade do desenvolvedor.
- Cross-module calls (REST clients entre módulos) não são testadas automaticamente — gap conhecido que exigiria WireMock ou testes end-to-end.
