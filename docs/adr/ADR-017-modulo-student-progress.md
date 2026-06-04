# ADR-017: Módulo de Student Progress

## Status
Accepted

## Context

O módulo `student_progress` rastreia o estado de aprendizado de cada aluno: matrículas em cursos, progresso em aulas individuais, tentativas de exercícios e trilhas de aprendizado. As decisões centrais giram em torno de como armazenar e atualizar estado progressivo de forma eficiente sem duplicar lógica de outros módulos.

---

## Decision 1: Contadores de progresso desnormalizados em `CL_COURSE_ENROLLMENT`

**What:** `CourseEnrollmentEntity` armazena `lessons_completed` (contagem) e `progress_percentage` (calculado) como colunas. Esses valores são atualizados explicitamente pelo método `updateProgress()` a cada aula concluída.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Calcular em tempo real via `COUNT(*) FROM CL_LESSON_PROGRESS` | Correto sempre, mas custoso em cursos com muitas aulas e muitos alunos |
| View ou coluna gerada (GENERATED) | PostgreSQL não suporta GENERATED para agregações cross-table |

**Motivation:** A tela de progresso do aluno exibe `progress_percentage` a cada acesso. Executar `COUNT(*)` em cada visualização seria inaceitável em escala. Desnormalizar torna a leitura O(1) ao custo de um update adicional por conclusão de aula.

**Consequences:**
- (+) Leituras de progresso são lookups simples por PK
- (+) `progress_percentage` disponível sem JOIN para exibição em listagens
- (-) `updateProgress()` precisa ser chamado explicitamente após cada aula concluída; se esquecido, o contador diverge
- (-) Correção de divergência requer recálculo manual ou job de reconciliação

---

## Decision 2: Threshold de 90% para conclusão automática de vídeo

**What:** Em `LessonProgressService.trackProgress()`, uma aula de vídeo é marcada como `completed = true` quando `watchTimeSeconds >= videoDurationSeconds * 0.9`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| 100% de conclusão | Pune usuários que pulam os últimos segundos de crédito/encerramento |
| Threshold configurável por aula | Mais flexível, mas adiciona campo extra na entidade e complexidade de gestão para instrutores |
| Apenas marcação manual (`POST /complete`) | Não garante engajamento real com o conteúdo |

**Motivation:** 90% é um equilíbrio entre garantir engajamento real com o conteúdo e não penalizar comportamentos legítimos como pular trechos de encerramento.

**Consequences:**
- (+) UX fluida: aluno não precisa assistir créditos finais para ganhar o progresso
- (+) Menos chamadas de API: progressão ocorre implicitamente no tracking de watch time
- (-) Um aluno que assiste 89% várias vezes nunca completa automaticamente; o endpoint `/complete` serve como fallback
- (-) O limite de 90% é hardcoded no serviço; mudá-lo requer código novo

---

## Decision 3: Upsert implícito em `trackProgress()` via `orElseGet()`

**What:** `LessonProgressService.trackProgress()` busca a entidade existente com `findByUserAndLesson()`. Se não existe, cria uma nova via `orElseGet()`. O comportamento é idempotente: chamar `trackProgress()` múltiplas vezes não duplica registros.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Endpoints separados para criar e atualizar progresso | Exige que o cliente saiba se o progresso já existe antes de decidir qual endpoint chamar |
| `INSERT ... ON CONFLICT DO UPDATE` via SQL nativo | Mais eficiente em termos de roundtrip, mas quebra o padrão de Active Record do Panache |

**Motivation:** O cliente não deve precisar rastrear se o progresso de uma aula foi iniciado. O upsert implícito simplifica o caller e garante idempotência.

**Consequences:**
- (+) API de tracking de progresso é simples de usar pelo cliente
- (+) Sem erro 409 ao tentar registrar progresso em aula já iniciada
- (-) A lógica de create-or-update usa dois roundtrips ao banco (SELECT + INSERT/UPDATE); em alta concorrência pode ocorrer violação de unique constraint

---

## Decision 4: Conclusão automática do enrollment ao atingir 100%

**What:** O método `updateProgress()` calcula `progressPercentage = lessonsCompleted / totalLessons * 100`. Quando o valor atinge 100%, `completedAt` é setado automaticamente. Não há endpoint separado de "concluir matrícula".

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Endpoint explícito `POST /enrollments/{id}/complete` | Coloca no cliente a responsabilidade de detectar que todas as aulas foram concluídas |
| Sem campo `completedAt` (inferir via progresso = 100) | Simplifica o schema, mas perde o timestamp exato de conclusão para emissão de certificados |

**Motivation:** A conclusão é consequência direta do progresso completo — não é uma ação separada do usuário. Automatizá-la garante que o `completedAt` seja registrado imediatamente.

**Consequences:**
- (+) Fluxo de certificado pode ser acionado em resposta ao `completedAt` sem lógica extra
- (+) Consistência garantida: `completedAt` nunca é `null` quando `progress_percentage = 100`
- (-) Sem mecanismo de "desconcluir" se uma aula nova for adicionada ao curso; `completedAt` permanece mesmo com progresso < 100

---

## Decision 5: Learning Paths como curadoria separada das matrículas

**What:** `CL_LEARNING_PATH` e `CL_LEARNING_PATH_COURSE` são entidades independentes de `CL_COURSE_ENROLLMENT`. Matricular-se em uma learning path não matricula automaticamente o aluno em cada curso.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Learning path como "super-enrollment" que cria matrículas em cascata | Exige rollback complexo se a inscrição na path falhar após criar algumas matrículas |
| Integrar curadoria diretamente no módulo `course` | Mistura responsabilidades de conteúdo com planejamento de aprendizado |

**Motivation:** Learning paths são planos de estudo sugeridos, não compromissos automáticos. Separar curadoria de matrícula mantém a granularidade do controle no aluno.

**Consequences:**
- (+) Aluno pode explorar learning paths sem criar matrículas indesejadas
- (+) Learning paths e matrículas evoluem de forma independente
- (-) Experiência "seguir um caminho de aprendizado" requer que o cliente crie matrículas individualmente
