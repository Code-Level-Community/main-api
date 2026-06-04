# ADR-016: Módulo de Course

## Status
Accepted

## Context

O módulo `course` gerencia o conteúdo principal da plataforma: cursos, módulos, aulas e exercícios. A hierarquia de conteúdo e o ciclo de vida de publicação são as duas preocupações centrais que geraram as decisões mais relevantes deste módulo.

---

## Decision 1: Ciclo de vida de cursos via state machine com 5 estados

**What:** O status de um curso percorre a sequência: `DRAFT → EXPERIMENTAL → COMMUNITY_APPROVED → EXPERT_APPROVED → ARCHIVED`. Cada transição tem pré-condições validadas no serviço. O método `publish()` avança de DRAFT para EXPERIMENTAL (instrutor) ou EXPERT_APPROVED diretamente (admin). O método `tryApprove()` avança de EXPERIMENTAL para COMMUNITY_APPROVED quando o threshold de aprovação é atingido.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| 2 estados (ACTIVE/ARCHIVED) | Perde a noção de fase experimental e aprovação comunitária, central ao modelo de negócio |
| Flags booleanas (`isPublished`, `isApproved`, `isArchived`) | Cria combinações inconsistentes (e.g., `isArchived = true` e `isPublished = true` simultaneamente) |

**Motivation:** O CodeLevel tem um processo de curadoria: instrutores submetem cursos que passam por aprovação comunitária antes da publicação oficial. Estados intermediários explícitos permitem que outros módulos (course_requests, reviews) ajam nos momentos corretos.

**Consequences:**
- (+) Transições inválidas detectadas no serviço antes de qualquer mutação no banco
- (+) Estado do curso expressa claramente em qual fase do processo de publicação ele está
- (-) Adicionar um novo estado exige rever todas as verificações de transição existentes
- (-) `tryApprove()` é chamado externamente; sem garantia de que será chamado após cada nova review

---

## Decision 2: Validação de conteúdo mínimo no publish

**What:** O método `publish()` verifica que o curso possui ao menos um módulo com ao menos uma aula antes de mudar o status para EXPERIMENTAL. Tentativa de publicar curso vazio lança `BusinessRuleException`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Sem validação (publicar vazio) | Cria cursos sem conteúdo visíveis para alunos |
| Validação via UI apenas | Frontend pode falhar ou ser contornado, permitindo cursos inválidos via API |

**Motivation:** Um curso sem módulos ou aulas não tem valor para nenhum aluno. Enforçar a regra no serviço garante que nenhum path de código possa publicar conteúdo vazio.

**Consequences:**
- (+) Invariante de conteúdo mínimo garantido independente de como a API é chamada
- (-) Instrutor requer criação de módulo e aula antes de tentar publicar; pode ser confuso sem documentação clara do fluxo

---

## Decision 3: Exercises com storage polimórfico via JSONB

**What:** A tabela `CL_EXERCISE` armazena dados de exercício em colunas JSONB: `quiz_data` (para QUIZ, com perguntas e alternativas) e `code_template` + `test_cases` (para exercícios de código). O tipo é discriminado pelo enum `ExerciseType`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Tabelas separadas por tipo (CL_QUIZ_EXERCISE, CL_CODE_EXERCISE) | Schema mais rigoroso, mas exige JOIN ou UNION em queries que listam exercícios de uma aula |
| Única coluna JSONB (`exercise_data`) com estrutura discriminada | Máxima flexibilidade, mas sem separação semântica entre os tipos |

**Motivation:** A maioria das operações acessa exercícios pelo `lesson_id` sem importar o tipo. Uma única tabela simplifica essas queries. As colunas JSONB separadas por tipo oferecem semântica melhor que um único campo genérico.

**Consequences:**
- (+) Queries de listagem por aula são simples (sem UNION ou JOIN)
- (+) Fácil adicionar campos a um tipo sem afetar o outro
- (-) Sem constraints de banco no conteúdo do JSONB; um exercício QUIZ com `quiz_data = null` é tecnicamente válido no banco
- (-) Queries que filtram dentro do JSONB requerem indexação específica do PostgreSQL

---

## Decision 4: Soft delete de cursos via status ARCHIVED

**What:** Cursos não são deletados do banco. O endpoint `archive` transiciona o status para `ARCHIVED` e a query `findAllEnabled()` filtra cursos com esse status. Hard delete só é permitido em cursos com status `DRAFT`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Hard delete sempre | Destrói histórico de matrículas, reviews, progresso e transações de XP associadas ao curso |
| Soft delete com flag `deleted_at` | Padrão mais comum, mas `ARCHIVED` como estado do enum é mais expressivo dentro do ciclo de vida do curso |

**Motivation:** Um curso com alunos matriculados não pode ser deletado sem perder dados de progresso. O status `ARCHIVED` oculta o curso de novos alunos sem destruir o histórico de quem já estava matriculado.

**Consequences:**
- (+) Histórico de matrículas e progresso preservado mesmo após "exclusão"
- (+) Alunos já matriculados em cursos arquivados ainda podem acessar seu progresso
- (-) Cursos arquivados acumulam no banco indefinidamente; pode exigir política de retenção futura
- (-) O filtro `ARCHIVED` precisa estar presente em toda query de listagem pública

---

## Decision 5: Verificação de ownership no nível de serviço

**What:** Operações de update, delete e publish validam no serviço que o `requesterId` (extraído do JWT pelo resource e passado ao serviço) é o `instructorId` do curso, ou que o usuário tem role `ROLE_ADMIN`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Verificação apenas via `@RolesAllowed` no resource | Permite que qualquer instrutor edite o curso de outro instrutor |
| Verificação no resource (antes de chamar o serviço) | Lógica de negócio vazando para a camada de transporte |

**Motivation:** A regra "só o dono ou um admin pode editar" é lógica de negócio, não de transporte. Colocá-la no serviço garante que seja respeitada independente de como o serviço é chamado.

**Consequences:**
- (+) Regra de ownership testável em service ITs sem contexto HTTP
- (+) Qualquer novo caller do serviço herda a proteção automaticamente
- (-) O serviço precisa receber `requesterId` e `isAdmin` como parâmetros adicionais, aumentando a assinatura dos métodos

---

## Decision 6: Ordenação de módulos e aulas por `order_position`

**What:** `ModuleEntity` e `LessonEntity` possuem campo `order_position INTEGER`. As queries ordenam por `order_position ASC NULLS LAST`. O gerenciamento da posição é responsabilidade do caller.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Ordenação por `created_at` | Simples, mas impede reordenação sem recriar entidades |
| Lista ordenada gerenciada pelo ORM (`@OrderColumn`) | Automático, mas acoplado à coleção JPA e difícil de manipular via queries diretas |

**Motivation:** Instrutores precisam reordenar o conteúdo do curso sem recriar aulas. Um campo explícito de posição permite reordenação via update simples. `NULLS LAST` garante que conteúdo sem posição apareça ao final.

**Consequences:**
- (+) Reordenação flexível sem recriar entidades
- (+) `NULLS LAST` torna posição nullable sem quebrar a query
- (-) Responsabilidade de manter posições únicas e sem lacunas é do caller; o sistema não previne `order_position = 5` em dois módulos do mesmo curso
