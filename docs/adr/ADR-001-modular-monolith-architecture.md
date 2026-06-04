# ADR-001: Arquitetura Monólito Modular

## Status
Accepted

## Context

O sistema precisa gerenciar 9 domínios distintos: autenticação e identidade, cursos, gamificação, progresso de estudantes, comunidade Q&A, integrações sociais, sugestões de cursos, feedbacks e certificados.

## Decision

Organizar a aplicação como um **monólito modular** com 9 módulos de domínio sob `com.codelevel.module`:

| Módulo | Responsabilidade |
|---|---|
| `identity` | Usuários, auth (JWT), papéis e permissões |
| `course` | Cursos, módulos, lições, exercícios, categorias, tags |
| `gamification` | Transações de XP, conquistas, níveis, streaks |
| `student_progress` | Matrículas, progresso em lições, tentativas de exercícios |
| `community` | Q&A: perguntas, respostas, votação |
| `integration` | Rastreamento de posts em redes sociais |
| `course_requests` | Sugestões de cursos e votação da comunidade |
| `reviews_feedbacks` | Avaliações de cursos e lições |
| `certificate` | Emissão de certificados |

Cada módulo possui fronteiras claras: não injeta serviços de outros módulos diretamente e expõe dados exclusivamente via endpoints REST públicos.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Microsserviços | Complexidade operacional elevada (service discovery, distributed tracing, eventual consistency) inadequada para o estágio atual |
| Monólito tradicional | Dificulta a evolução independente dos domínios; sem separação de fronteiras entre componentes |

## Motivation

Deploy simples de um único artefato com fronteiras de módulo que permitem evolução independente e eventual extração como microsserviços sem grandes refatorações.

## Consequences

### Positive
- Deploy simples: um único artefato, sem orquestração de contêineres distribuídos.
- Sem latência de rede em chamadas inter-módulo (mesmo JVM).
- Compatível com compilação GraalVM native (um binário só).
- Fronteiras de módulo preparadas para eventual extração como microsserviço no futuro.
- Transações JPA cross-módulo possíveis quando necessário (mesmo datasource).

### Negative / Trade-offs
- O artefato cresce conforme o sistema expande; builds ficam mais longos.
- Um bug crítico em um módulo pode derrubar toda a aplicação.
- Disciplina manual necessária para não violar fronteiras (sem enforcement automático de compilador).
- Escalonamento granular por domínio não é possível sem separar em serviços.