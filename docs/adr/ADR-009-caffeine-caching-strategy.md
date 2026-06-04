# ADR-009: Estratégia de Cache com Caffeine

## Status
Accepted

## Context

Leituras de dados de usuário ocorrem em praticamente toda requisição autenticada (resolução do usuário atual, verificação de papéis, listagens). Consultar o banco em cada requisição gera carga desnecessária para dados que mudam raramente.

## Decision

Utilizar **Caffeine como cache in-process** com dois caches de configurações distintas, gerenciados via anotações do `quarkus-cache`:

### Configuração dos Caches (`application.yml`)

```yaml
quarkus:
  cache:
    caffeine:
      user-cache:
        expire-after-write: 30M
        expire-after-access: 10M
        maximum-size: 5000
      user-list-cache:
        expire-after-write: 2M
        expire-after-access: 1M
        maximum-size: 10
```

### Uso no código

```java
@CacheResult(cacheName = "user-cache")
public UserEntity findUser(UUID userId) { ... }

@CacheInvalidate(cacheName = "user-cache")
@CacheInvalidate(cacheName = "user-list-cache")
public void updateUser(UUID userId, ...) { ... }

@CacheInvalidateAll(cacheName = "user-list-cache")
public void createUser(...) { ... }
```

Regra: toda operação de escrita invalida as entradas de cache correspondentes.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Sem cache | Banco vira gargalo em cenários de leitura intensa; dados de usuário consultados em praticamente toda request |
| Redis (cache distribuído) | Consistência entre instâncias, mas requer infraestrutura extra e latência de rede |

## Motivation

Zero latência de rede (cache in-process), sem dependência de infraestrutura externa, com API declarativa que não espalha código de cache pela aplicação.

## Consequences

### Positive
- Zero latência de rede: cache reside no mesmo processo da aplicação.
- Sem dependência de infraestrutura externa (Redis, Memcached).
- API declarativa: `@CacheResult`, `@CacheInvalidate`, `@CacheInvalidateAll` — sem código de cache espalhado.
- Compatível com GraalVM native.
- TTL diferenciado por tipo de dado (entidade vs lista).

### Negative / Trade-offs
- Cache não é compartilhado entre instâncias: em deploy com múltiplas réplicas, cada instância tem seu próprio cache — dados podem divergir por até 30 minutos.
- TTLs fixos em configuração: ajuste requer redeploy (sem hot-reload de TTL).
- Invalidação manual obrigatória em todas as operações de escrita — fácil esquecer e ter dados stale.
- `user-list-cache` com `maximum-size: 10` é muito pequeno para sistemas com muitos filtros/paginações distintos.
- Caffeine não sobrevive a restart da aplicação: warm-up necessário após redeploy.
