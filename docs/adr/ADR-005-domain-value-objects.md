# ADR-005: Value Objects para Modelagem de Domínio

## Status
Accepted

## Context

Sem value objects, campos de domínio como e-mail, senha e nome completo circulam como `String` por toda a aplicação. Validações são feitas em múltiplos lugares (controller, service, entity) ou esquecidas. Qualquer `String` pode ser passada onde um e-mail é esperado — sem garantia de invariante.

## Decision

Campos de domínio com regras de negócio são encapsulados como **value objects** implementados como Java `record`, com validação obrigatória no construtor compacto:

```java
public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            throw new BusinessRuleException("Invalid email: " + value);
        }
        value = value.toLowerCase().strip();
    }
}

public record Password(String value) {
    public Password {
        if (value == null || value.length() < 6) {
            throw new BusinessRuleException("Password must have at least 6 characters");
        }
    }
}
```

O aggregate root de domínio (`User`) encapsula a entidade JPA e expõe a lógica de negócio usando esses value objects:

```java
public final class User {
    private final UserEntity entity;

    public User(UserEntity entity) { this.entity = entity; }

    public EmailAddress email() { return new EmailAddress(entity.email); }
    public boolean matchPass(String raw, PasswordHasher hasher) {
        return hasher.verify(raw, entity.passwordHash);
    }
}
```

Regras:
- Value objects lançam `BusinessRuleException` (domínio) — nunca `IllegalArgumentException` ou exceções HTTP.
- Value objects são imutáveis e sem identidade — igualdade por valor (`record` garante isso).
- O objeto de domínio `User` wrappa o `UserEntity` e é o ponto de acesso a lógica de negócio.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Validação em anotações Bean Validation (`@Email`, `@NotNull`) | Validação acontece fora do domínio (framework); pode ser ignorada em chamadas diretas ao serviço; menos expressivo no sistema de tipos |
| Strings primitivas com validação no serviço | Validação duplicada em cada ponto de entrada; qualquer `String` passa no compilador sem garantia de invariante |

## Motivation

Invariantes de domínio garantidos em tempo de construção: impossível ter um `EmailAddress` inválido em circulação. O sistema de tipos expressa intenção (`void send(EmailAddress to)` vs `void send(String to)`).

## Consequences

### Positive
- Invariantes de domínio garantidos em tempo de construção: impossível ter um `EmailAddress` inválido em circulação.
- Validação centralizada — não se repete em controller, service e entity.
- Sistema de tipos expressivo: `void send(EmailAddress to)` vs `void send(String to)`.
- Records Java são imutáveis por padrão e têm `equals`/`hashCode`/`toString` gerados.
- Compatível com GraalVM native (sem reflexão).

### Negative / Trade-offs
- Overhead de criação de objetos wrapper para campos simples.
- Curva de aprendizado para desenvolvedores acostumados a trabalhar com primitivos.
- Conversão necessária ao salvar na entidade JPA: `entity.email = emailAddress.value()`.
- Exceções de validação lançadas no construtor podem surpreender devs que esperam validação em camada de serviço.