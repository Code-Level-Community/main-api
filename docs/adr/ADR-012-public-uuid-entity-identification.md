# ADR-012: UUID Público para Identificação de Entidades

## Status
Accepted

## Context

Entidades JPA usam naturalmente chaves primárias numéricas sequenciais (`Long id`) para performance de índice e join no banco. Expor esses IDs na API pública cria dois problemas: enumeração (IDs sequenciais permitem iterar sobre recursos) e acoplamento de schema (clientes dependem do ID interno).

## Decision

Adotar o padrão de **ID duplo**: chave primária interna (`Long id`) para eficiência de banco, e identificador público (`UUID publicId`) exposto na API:

```java
@Entity
@Table(name = "CL_USER")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "cl_user_seq", allocationSize = 50)
    public Long id;                         // PK interna: nunca exposta via API

    @Column(name = "u_public_id", unique = true, nullable = false)
    public UUID publicId;                   // ID público: usado em todas as URLs

    @PrePersist
    void generatePublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
}
```

### Regras de uso
- **APIs** referenciam entidades exclusivamente pelo `publicId`: `GET /api/users/{publicId}`.
- Joins e foreign keys internas no banco usam `Long id`.
- Respostas HTTP nunca incluem o campo `id` (Long) — apenas `publicId`.
- O `Long id` nunca deve aparecer em tokens JWT, responses de API, ou logs acessíveis externamente.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| UUID como PK (substituir Long) | UUIDs como PK no PostgreSQL têm impacto em performance de índice B-tree comparado a inteiros sequenciais |
| Long sequencial exposto na API | Enumeração: IDs previsíveis permitem iterar sobre recursos; expõe contagem de registros e schema interno |

## Motivation

Proteção contra enumeração com UUIDs imprevisíveis, sem abrir mão da performance de índice B-tree do Long como PK interna. Desacoplamento do schema interno da interface pública da API.

## Consequences

### Positive
- Protege contra enumeração: UUIDs aleatórios são impraticáveis de adivinhar ou iterar.
- Desacopla o schema interno do banco da interface pública da API.
- PKs Long mantêm performance ótima de índice B-tree e joins no PostgreSQL.
- `UUID.randomUUID()` gerado no `@PrePersist` é seguro e não requer sequência extra.
- Permite mover registros entre bancos sem quebrar URLs públicas.

### Negative / Trade-offs
- Duas colunas de ID por entidade: mais espaço em disco e índices adicionais.
- Queries por `publicId` (`WHERE public_id = ?`) são marginalmente mais lentas que por PK (`WHERE id = ?`), mesmo com índice único.
- Desenvolvedores precisam lembrar de nunca expor o `Long id` — sem enforcement automático do compilador.
- Foreign keys no banco entre módulos precisam decidir: referenciar por `Long id` (eficiência) ou `UUID publicId` (segurança).
