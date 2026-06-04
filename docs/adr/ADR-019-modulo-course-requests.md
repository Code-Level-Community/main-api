# ADR-019: Módulo de Course Requests

## Status
Accepted

## Context

O módulo `course_requests` implementa o processo de sugestão democrática de cursos: membros da comunidade propõem tópicos, votam nas sugestões e instrutores voluntariamente aceitam criar os cursos aprovados. É um fluxo de trabalho colaborativo com múltiplos atores e estados bem definidos.

---

## Decision 1: Aprovação automática via threshold de votação

**What:** O serviço define `MIN_VOTES_FOR_APPROVAL = 20` e `APPROVAL_THRESHOLD = 70.0%`. A cada voto, `checkAutoApproval()` é chamado internamente: se o total atingiu o mínimo e a porcentagem de aprovação ultrapassou o threshold, o status transiciona automaticamente de `PENDING` para `APPROVED`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Aprovação manual apenas (admin aprova) | Controle total, mas cria gargalo no admin para cada sugestão popular |
| Aprovação automática por upvotes absolutos (sem porcentagem) | Fácil de manipular com muitos downvotes; a razão de aprovação é métrica mais saudável |
| Threshold configurável no banco | Mais flexível, mas adiciona complexidade de gestão para um valor que raramente mudará |

**Motivation:** O CodeLevel é uma plataforma comunitária. Delegar a curadoria à comunidade é central ao modelo. Thresholds hardcoded são suficientes para o MVP.

**Consequences:**
- (+) Admin não se torna gargalo para aprovação de sugestões populares
- (+) Transição transparente: o sistema explica objetivamente por que uma sugestão foi aprovada
- (-) Constantes hardcoded não são configuráveis sem redeploy
- (-) Um ator mal-intencionado com múltiplas contas pode manipular a votação; sem mecanismo de detecção de fraude

---

## Decision 2: Tabela de votos dedicada (não polimórfica)

**What:** Course requests têm sua própria tabela `CL_COURSE_REQUEST_VOTE`, separada do sistema de votação polimórfico do módulo `community`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Reutilizar `CL_VOTE` do módulo `community` com `votable_type = COURSE_REQUEST` | Cria dependência do módulo `course_requests` para o schema do módulo `community`, violando fronteiras de módulo |
| Compartilhar entidade via `shared` package | Exporia entidade de persistência como contrato compartilhado, quebrando encapsulamento de módulo |

**Motivation:** A arquitetura proíbe dependências diretas entre módulos (ADR-003). Uma tabela de votos própria mantém o módulo `course_requests` completamente autossuficiente.

**Consequences:**
- (+) `course_requests` pode ser desenvolvido, testado e extraído sem dependência do módulo `community`
- (+) Schema dedicado permite adicionar campos específicos ao voto sem afetar outros módulos
- (-) Dois sistemas de votação com schemas similares — risco de divergência na evolução
- (-) Desenvolvedor novo pode se perguntar por que não são unificados

---

## Decision 3: Mecanismo de give-up para instrutores

**What:** O endpoint `POST /{id}/give-up` permite que um instrutor em `IN_PROGRESS` devolva a sugestão para o estado `APPROVED`, limpando o `assigned_instructor_id`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Sem give-up | Sugestões abandonadas ficam travadas em `IN_PROGRESS` indefinidamente |
| Timeout automático (job que reverte após N dias) | Mais automático, mas adiciona dependência de scheduler e definição de SLA |
| Reassignação direta pelo admin | Mais controlada, mas cria um novo tipo de transição e burocracia |

**Motivation:** Instrutores são voluntários. Um mecanismo explícito de devolução é mais honesto que forçar uma responsabilidade e mais simples que um scheduler.

**Consequences:**
- (+) Sugestões não ficam bloqueadas indefinidamente
- (+) Sem penalidade automática ao instrutor que usa o give-up
- (-) Sem notificação automática aos votantes de que o instrutor desistiu
- (-) O histórico de `assigned_instructor_id` é perdido no give-up; sem auditoria de quem já tentou

---

## Decision 4: Rastreamento do curso criado via `created_course_id`

**What:** Ao completar (`POST /{id}/complete`), o instrutor fornece o `created_course_id` que é armazenado na solicitação. Isso cria um link auditável entre a sugestão comunitária e o curso efetivamente criado.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Link via campo no curso (o curso referencia a solicitação) | Colocaria a referência no módulo `course`, que não deve depender do módulo `course_requests` |
| Sem link explícito | Perde a rastreabilidade de quais cursos nasceram de sugestões da comunidade |

**Motivation:** A rastreabilidade "esta sugestão gerou este curso" é valiosa para a comunidade (transparência) e para estatísticas da plataforma.

**Consequences:**
- (+) Comunidade pode verificar que sua sugestão foi atendida e qual curso foi criado
- (+) Dados para métricas: taxa de conversão sugestão→curso, tempo médio até conclusão
- (-) `created_course_id` não tem foreign key verificável (cross-module); um course_id inválido pode ser submetido sem erro de banco
- (-) Se o curso criado for arquivado/deletado, o link aponta para recurso inexistente

---

## Decision 5: Estado `IN_PROGRESS` com instructor único

**What:** Apenas um instrutor pode estar `IN_PROGRESS` em uma solicitação por vez. O `accept()` transiciona de `APPROVED` para `IN_PROGRESS` e armazena o `instructorId` no campo `assigned_instructor_id`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Múltiplos instrutores trabalhando na mesma solicitação | Sem modelo de ownership claro pode resultar em dois cursos sendo criados para a mesma sugestão |
| Fila de instrutores interessados | Mais elaborado, mas desnecessário para o MVP |

**Motivation:** Um único instructor como responsável evita duplicação de trabalho e torna a accountability clara. O mecanismo de `give-up` garante que o bloqueio seja temporário.

**Consequences:**
- (+) Sem cursos duplicados criados para a mesma sugestão
- (+) Responsabilidade clara: `assigned_instructor_id` identifica quem está trabalhando
- (-) Se o instrutor sumir sem usar give-up, a solicitação fica bloqueada; requer intervenção de admin para forçar reset
