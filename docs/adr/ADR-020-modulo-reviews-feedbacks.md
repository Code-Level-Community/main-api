# ADR-020: Módulo de Reviews & Feedbacks

## Status
Accepted

## Context

O módulo `reviews_feedbacks` gerencia dois tipos distintos de avaliação de conteúdo: reviews de cursos (com nota 1–5 e impacto na aprovação do curso) e feedbacks de aulas (binário helpful/not helpful). As decisões principais envolvem como garantir a integridade das avaliações e como coordenar com o módulo `course` sem criar dependências diretas.

---

## Decision 1: Validação de existência do curso via REST client

**What:** O método `CourseReviewService.create()` chama `CourseRestClient.getCourse(courseId)` via REST síncrono antes de persistir o review. Se o curso não existe, o client lança `ResourceNotFound`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Sem validação (confiar no FK do banco) | Banco não tem FK cross-module (ADR-003); um `course_id` inválido seria aceito silenciosamente |
| Validação lazy (detectar na leitura) | Reviews orphans acumulam até alguém tentar listá-los; difícil de diagnosticar |
| Evento de domínio (course deleted → delete reviews) | Desacopla, mas exige infraestrutura de eventos ainda não implementada |

**Motivation:** A integridade referencial entre módulos não pode depender do banco. Validar via REST no momento da criação garante que reviews sempre referenciam cursos existentes.

**Consequences:**
- (+) Impossível criar review para curso inexistente
- (+) Erro imediato e descritivo ao tentar avaliar curso que não existe
- (-) O módulo `reviews_feedbacks` é acoplado à disponibilidade do módulo `course`
- (-) Latência adicional por roundtrip HTTP mesmo sendo same-JVM
- (-) Se o curso for arquivado após o review ser criado, o review permanece sem invalidação

---

## Decision 2: Sistema dual de avaliação: rating numérico (curso) vs booleano (aula)

**What:** Reviews de cursos usam escala numérica 1–5 com campo `is_positive` booleano derivado (1–3 = negativo, 4–5 = positivo). Feedbacks de aulas usam apenas `is_helpful` booleano, sem nota numérica.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Rating numérico para ambos | Permite rankings de aulas, mas aulas são unidades menores onde nota granular adiciona pouco valor |
| Apenas booleano para ambos | Simples, mas perde a granularidade essencial para o mecanismo de aprovação de cursos |
| NPS (0–10) para cursos | Mais granular, mas `is_positive` derivado seria calculado com threshold diferente |

**Motivation:** Cursos requerem nota numérica porque `tryApprove()` avalia a taxa de reviews positivos para transicionar o curso de EXPERIMENTAL para COMMUNITY_APPROVED. Aulas são avaliadas de forma mais leve para orientar melhorias sem sobrecarregar o aluno.

**Consequences:**
- (+) Avaliação proporcional ao peso do conteúdo: curso (avaliação formal), aula (feedback rápido)
- (+) `is_positive` no review de curso é calculável pelo range da nota e persistido para queries eficientes
- (-) Dois tipos de entidade com lógica de validação parcialmente similar; alguma duplicação inevitável
- (-) O significado de "positivo" (rating ≥ 4) pode mudar sem que seja óbvio onde está o threshold no código

---

## Decision 3: Feedback de aula é imutável

**What:** `CL_LESSON_FEEDBACK` não possui coluna `updated_at`. Uma vez criado, o feedback não pode ser atualizado. Não há endpoint de update para `LessonFeedback`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Feedback mutável (como o review de curso) | Permite corrigir avaliação impulsiva, mas adiciona complexidade sem benefício proporcional para um sinal binário |
| Delete + re-create | A unique constraint impede criar um segundo feedback para a mesma aula após deletar o primeiro |

**Motivation:** O feedback de aula é um sinal de qualidade, não uma avaliação formal. A imutabilidade simplifica o schema e o serviço.

**Consequences:**
- (+) Schema simplificado (sem `updated_at`)
- (+) Serviço sem lógica de update para feedback
- (-) Aluno preso a uma avaliação errada; sem mecanismo de correção
- (-) A ausência de update não é documentada na API; pode surpreender clientes que esperam comportamento mutável similar ao review de curso

---

## Decision 4: Rating com validação em dois níveis: domínio e banco

**What:** O domain object `Rating` lança `BusinessRuleException` se o score não estiver entre 1 e 5. A coluna `rf_rating` em `CL_COURSE_REVIEW` tem `CHECK (rf_rating BETWEEN 1 AND 5)` no banco.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Validação apenas no domínio | Inserts diretos no banco ou bugs que contornem o domain object aceitariam valores inválidos |
| Validação apenas no banco (constraint) | A exceção de violação de constraint é menos descritiva que `BusinessRuleException` |

**Motivation:** Defesa em profundidade: a camada de domínio fornece mensagem descritiva; a constraint de banco protege contra qualquer bypass da aplicação.

**Consequences:**
- (+) Proteção em dois níveis: UX com mensagem clara + integridade no banco
- (+) Alterações diretas no banco que violem a regra são prevenidas pela constraint
- (-) Se o range mudar (ex: escala 1–10), a regra precisa ser atualizada em dois lugares

---

## Decision 5: Uma avaliação por usuário por curso/aula

**What:** `CL_COURSE_REVIEW` tem `UNIQUE(user_id, course_id)` e `CL_LESSON_FEEDBACK` tem `UNIQUE(user_id, lesson_id)`. Tentativa de criar segundo review/feedback lança `ResourceAlreadyExists` (409).

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Múltiplas avaliações por usuário (histórico temporal) | Complica o cálculo de `average_rating` (última? média? todas?) |
| Update silencioso | Sobrescreve automaticamente em vez de retornar 409; viola a semântica REST de POST |

**Motivation:** Múltiplas avaliações do mesmo usuário distorceriam o `average_rating` do curso. O usuário pode atualizar sua avaliação via `PUT /{id}`.

**Consequences:**
- (+) Impossível distorcer a média com múltiplos reviews do mesmo usuário
- (+) Constraint de banco previne race condition de duplo-submit
- (-) O cliente precisa tratar 409 e dirigir o usuário ao endpoint de update
