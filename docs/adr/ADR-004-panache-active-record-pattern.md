# ADR-004: Padrão Active Record com Panache para ORM

## Status
Accepted

## Context

O projeto usa Quarkus com Hibernate ORM. As duas principais abstrações sobre JPA são Repository Pattern e Active Record. O Repository Pattern exige criar uma classe extra por entidade; o Active Record concentra query e entidade no mesmo arquivo.

## Decision

Todas as entidades JPA estendem `PanacheEntityBase` e incluem métodos de consulta estáticos:

```java
@Entity
@Table(name = "CL_USER")
public class UserEntity extends PanacheEntityBase {

    @Id
    public Long id;

    @Column(name = "u_username")
    public String username;

    public static Optional<UserEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public static List<UserEntity> findAllEnabled() {
        return find("enabled", true).list();
    }
}
```

Regras complementares:
- Queries retornam `Optional<T>` em vez de `null` quando o resultado pode não existir.
- Nomes de query usam JPQL simplificado do Panache (`"username = ?1"` ou shorthand `"username", value`).
- IDs primários são `Long`; exposto publicamente via `UUID publicId`.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Repository Pattern | Requer classe extra por entidade; mais verboso sem ganho real para queries simples; padrão Spring Data não idiomático no Quarkus |
| JPA puro (sem Panache) | `EntityManager` explícito em cada serviço; mais verboso; sem API fluente |

## Motivation

Menos boilerplate e integração nativa com o ecossistema Quarkus (dev mode, hot reload, native image). A API fluente do Panache cobre a maioria dos casos de uso sem código repetitivo.

## Consequences

### Positive
- Elimina repositórios separados: menos arquivos, menos classes, menos indireção.
- Integração nativa com o Quarkus dev mode (hot reload, dev UI).
- Compatível com GraalVM native image sem configuração adicional.
- API fluente do Panache (`find`, `list`, `stream`, `count`, `delete`) cobre a maioria dos casos de uso.
- `Optional` como retorno elimina NPE em buscas que podem não encontrar resultado.

### Negative / Trade-offs
- Lógica de query acoplada à entidade dificulta testar queries em isolamento sem banco real.
- Mockar `UserEntity.findByUsername()` em testes unitários requer PowerMock ou Quarkus test container.
- Para queries muito complexas (joins múltiplos, subqueries), o Panache shorthand fica menos legível do que JPQL ou Criteria API explícitos.
- Padrão diverge do ecossistema Spring, o que pode criar atrito para devs com background Spring Data.