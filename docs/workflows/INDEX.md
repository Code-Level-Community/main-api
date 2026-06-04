# Workflows — Índice e Histórico de Versões

Todos os diagramas usam sintaxe [Mermaid](https://mermaid.js.org/).  
Cada arquivo `.mermaid` contém metadados de versão nas primeiras linhas (`%% version`, `%% updated`, `%% description`).

---

## Inventário

| Arquivo | Versão | Tipo | Módulo(s) | Descrição |
|---|---|---|---|---|
| [course-status.mermaid](course-status.mermaid) | 1.1.0 | State Machine | course | Ciclo de vida de um curso: DRAFT → publicação → aprovação → ARCHIVED |
| [course-request.mermaid](course-request.mermaid) | 1.1.0 | State Machine | course_requests | Sugestão de curso pela comunidade: votação → aceitação → conclusão |
| [certificate.mermaid](certificate.mermaid) | 1.0.0 | State Machine | certificate | Emissão assíncrona de certificado: PENDING → PROCESSING → SENT/FAILED |
| [social-media-post.mermaid](social-media-post.mermaid) | 1.0.0 | State Machine | integration | Post em rede social: PENDING → POSTED/FAILED, com retry |
| [auth-flow.mermaid](auth-flow.mermaid) | 1.0.0 | Sequence | identity | Signup, login (JWT RSA + refresh token), refresh e logout |
| [xp-award.mermaid](xp-award.mermaid) | 1.0.0 | Flowchart | gamification | Concessão de XP com idempotência, bônus de streak e cap diário |
| [streak-activity.mermaid](streak-activity.mermaid) | 1.0.0 | Flowchart | gamification | Registro de atividade diária com extensão, reset e streak freeze |
| [achievement-unlock.mermaid](achievement-unlock.mermaid) | 1.0.0 | Flowchart | gamification | Verificação e desbloqueio de achievements com cascade |
| [enrollment-progress.mermaid](enrollment-progress.mermaid) | 1.0.0 | Flowchart | student_progress | Matrícula, tracking de progresso de aula (90%) e conclusão automática |
| [exercise-attempt.mermaid](exercise-attempt.mermaid) | 1.0.0 | Flowchart | student_progress | Tentativa de exercício com max_attempts, XP com penalidade por erro |

---

## Tipos de Diagrama

| Tipo | Quando usar |
|---|---|
| **State Machine** (`stateDiagram-v2`) | Entidades com estados bem definidos e transições validadas |
| **Sequence** (`sequenceDiagram`) | Fluxos com múltiplos atores (usuário, resource, service, DB) |
| **Flowchart** (`flowchart TD`) | Algoritmos com decisões, loops e ramificações de uma mesma operação |

---

## Histórico de Alterações

### course-status.mermaid
- **1.1.0** (2026-05-08): Adicionado caminho `DRAFT → EXPERT_APPROVED` (publicação direta por admin); hard delete restrito ao estado DRAFT explicitado; notas revisadas com regras reais do código.
- **1.0.0** (inicial): Criação com os 5 estados e notas de contexto. Arquivo original em `docs/course-status-workflow.mermaid`.

### course-request.mermaid
- **1.1.0** (2026-05-08): Constantes `MIN_VOTES = 20` e `APPROVAL_THRESHOLD = 70%` explicitadas; nota sobre single-instructor adicionada; transição `give-up` destacada com limpeza do `assigned_instructor_id`.
- **1.0.0** (inicial): Criação com os 5 estados e transições básicas. Arquivo original em `docs/course-request-workflow.mermaid`.

### certificate.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre commit-before-dispatch, snapshot de dados do usuário e retry via re-solicitação.

### social-media-post.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre mock toggle, processo em duas fases do Instagram e comportamento de retry.

### auth-flow.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre signup com `@PrePersist`, login com revogação de sessão anterior, refresh com rotação de token e logout.

### xp-award.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre idempotência, bônus 1.5× (streak ≥ 7 dias), cap de 500 XP/dia com truncamento e detecção de level-up.

### streak-activity.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre primeira atividade, idempotência no mesmo dia, extensão, reset por gap e proteção via freeze.

### achievement-unlock.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre `checkAndUnlock`, concessão de XP pós-unlock e cascade recursivo via `ACHIEVEMENT_UNLOCKED`.

### enrollment-progress.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre matrícula, upsert de progresso, threshold de 90% para vídeo, conclusão automática do enrollment e solicitação de certificado.

### exercise-attempt.mermaid
- **1.0.0** (2026-05-08): Criação. Cobre controle de `maxAttempts`, avaliação, fórmula de XP com penalidade por tentativas erradas e marcação de aula como concluída.
