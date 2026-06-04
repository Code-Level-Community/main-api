# ADR-010: Flyway Migrations com H2 em Dev e PostgreSQL em Prod

## Status
Accepted

## Context

O gerenciamento de schema de banco de dados precisa atender a dois contextos opostos: desenvolvimento (ciclo rápido, banco descartável) e produção (mudanças rastreáveis, auditáveis, reprodutíveis). Usar PostgreSQL em dev resolve a divergência de dialeto, mas exige Docker ou serviço externo.

## Decision

Adotar estratégia de persistência dupla com uma camada de migration unificada:

### Desenvolvimento — H2 in-memory

```yaml
quarkus:
  datasource:
    db-kind: h2
    jdbc:
      url: jdbc:h2:mem:codelevel;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  hibernate-orm:
    database:
      generation: drop-and-create
```

- Banco em memória: zero dependências externas, startup instantâneo.
- `MODE=PostgreSQL`: compatibilidade de tipos e sintaxe com PostgreSQL.
- `drop-and-create`: schema sempre limpo — migrations Flyway não executam em dev.

### Produção — PostgreSQL com Flyway

```yaml
quarkus:
  datasource:
    db-kind: postgresql
    jdbc:
      url: ${DB_URL}
  flyway:
    migrate-at-start: true
    locations: classpath:db/migration
  hibernate-orm:
    database:
      generation: none
```

- Migrations em `src/main/resources/db/migration/` com convenção `V{N}__{descricao}.sql`.
- Flyway aplica migrations pendentes automaticamente no startup.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| PostgreSQL em dev (via Docker) | Resolve divergência de dialeto, mas aumenta atrito no setup inicial |
| Schema management via Hibernate `drop-and-create` em prod | Destrói dados de produção a cada deploy; sem auditoria de mudanças |
| Liquibase | Flyway é mais simples para o modelo de migrations lineares deste projeto |

## Motivation

Dev sem dependências externas para onboarding rápido. Produção com histórico de migrations auditável em git, aplicado de forma reprodutível em qualquer ambiente.

## Consequences

### Positive
- Dev sem dependências externas: qualquer dev faz `./mvnw quarkus:dev` e está pronto.
- Schema de produção totalmente auditável via histórico git dos arquivos de migration.
- Flyway previne aplicação de migrations fora de ordem ou duplicadas.
- `drop-and-create` em dev garante que o schema esteja sempre sincronizado com as entidades JPA.

### Negative / Trade-offs
- `MODE=PostgreSQL` do H2 não é 100% compatível — tipos específicos do PostgreSQL (JSONB, arrays, `gen_random_uuid()`) quebram em dev.
- O `drop-and-create` em dev significa que dados de teste são perdidos a cada restart.
- Divergência de comportamento entre H2 e PostgreSQL pode mascarar bugs que só aparecem em produção.
- Migrations são irreversíveis por padrão no Flyway — rollback requer migration de "undo" adicional.
