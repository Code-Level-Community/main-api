# ADR-002: Estrutura Interna em Camadas dos Módulos

## Status
Accepted

## Context

Sem uma convenção de organização de código, cada desenvolvedor pode estruturar módulos de forma diferente, tornando o codebase difícil de navegar e mantendo lógica de negócio misturada com código HTTP ou de persistência.

## Decision

Cada módulo segue esta estrutura de diretórios obrigatória:

```
module/{nome}/
├── http/rest/
│   ├── routes/          ← Controladores JAX-RS (*Resource.java, @Path)
│   ├── dto/             ← DTOs de request e response HTTP
│   ├── handler/         ← Exception mappers (@Provider ExceptionMapper)
│   └── mapper/          ← Conversão Entity ↔ DTO
├── persistence/
│   ├── resource/        ← Serviços de aplicação (@ApplicationScoped, @Transactional)
│   │   └── dto/         ← DTOs internos de serviço (records)
│   └── entity/          ← Entidades JPA (extends PanacheEntityBase)
│       └── enums/       ← Enumerações de domínio
└── domain/              ← Objetos de valor e lógica de negócio pura
```

Regras de dependência entre camadas:
- `http/rest/routes/` depende de `persistence/resource/` e `http/rest/mapper/`.
- `persistence/resource/` depende de `persistence/entity/` e `domain/`.
- `domain/` não depende de nenhuma outra camada.
- `http/rest/` nunca acessa `persistence/entity/` diretamente.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Estrutura livre por módulo | Cada dev organiza diferente; codebase imprevisível e difícil de navegar |
| Estrutura plana (sem camadas) | Mistura lógica HTTP, negócio e persistência; impede testes isolados |

## Motivation

Previsibilidade: qualquer dev sabe onde encontrar um arquivo por convenção de nome e camada, sem precisar buscar. Controladores ficam finos; lógica de negócio é testável sem contexto HTTP.

## Consequences

### Positive
- Localização previsível de qualquer arquivo por convenção de nome e camada.
- Controladores ficam finos: recebem request, delegam ao serviço, retornam response.
- Serviços são testáveis sem contexto HTTP.
- Exception mappers isolados em `handler/` tornam o tratamento de erros auditável.
- A camada `domain/` pode conter lógica pura testável sem Quarkus ou banco.

### Negative / Trade-offs
- Verbosidade de diretórios para funcionalidades simples (CRUD básico exige 4+ arquivos).
- Curva de aprendizado inicial para desenvolvedores novos no padrão.
- Distinção `persistence/resource/dto/` vs `http/rest/dto/` pode confundir; exige documentação clara de quando usar cada um.