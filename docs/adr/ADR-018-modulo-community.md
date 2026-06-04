# ADR-018: Módulo de Community

## Status
Accepted

## Context

O módulo `community` implementa o sistema de Q&A da plataforma: perguntas, respostas e votação. As principais decisões giram em torno de como modelar votação de forma eficiente e como lidar com os contadores de popularidade que guiam a exibição do conteúdo.

---

## Decision 1: Votação polimórfica via tabela única com discriminador ENUM

**What:** A tabela `CL_VOTE` armazena votos tanto em perguntas quanto em respostas. O tipo do alvo é discriminado pela coluna `votable_type` (enum `VotableType: QUESTION | ANSWER`) e `votable_id` armazena o ID do alvo. A restrição `UNIQUE(user_id, votable_type, votable_id)` previne voto duplo em nível de banco.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Tabelas separadas (`CL_QUESTION_VOTE` e `CL_ANSWER_VOTE`) | Duplica toda a lógica de votação e impede queries unificadas |
| Foreign keys nullable (`question_id` e `answer_id` com CHECK) | CHECK constraint complexo; não escala bem para novos tipos votáveis |

**Motivation:** A lógica de votação é idêntica para perguntas e respostas. Uma única tabela com discriminador ENUM elimina duplicação. Adicionar novos tipos votáveis requer apenas um novo valor no enum.

**Consequences:**
- (+) Lógica de votação implementada uma vez, funciona para todos os tipos votáveis
- (+) Query "todos os votos do usuário X" é direta, sem UNION
- (-) Foreign key verificável pelo banco não é possível com `votable_id` genérico; integridade referencial é responsabilidade da aplicação
- (-) Remoção de uma pergunta não causa cascade delete automático dos votos; requer delete manual

---

## Decision 2: Contadores de votos desnormalizados nas entidades

**What:** `QuestionEntity` armazena `up_votes` e `down_votes` como colunas. `AnswerEntity` também. O método `vote()` incrementa o contador correspondente após inserir o `VoteEntity` via `UPDATE SET up_votes = up_votes + 1` atômico.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| `COUNT(*) FROM CL_VOTE WHERE votable_type = ? AND votable_id = ?` em cada leitura | Custoso em listagens onde todas as perguntas precisam do contador |
| Cache de contadores | Elimina queries repetidas, mas adiciona complexidade de invalidação a cada novo voto |

**Motivation:** Listagens de perguntas por popularidade exigem o contador para ordenação. Executar um COUNT por pergunta tornaria qualquer listagem um problema de N+1.

**Consequences:**
- (+) `ORDER BY up_votes DESC` é eficiente com índice na coluna
- (+) Leituras de perguntas não requerem JOINs ou subqueries para contagem
- (-) Em alta concorrência, dois votos simultâneos podem causar race condition; mitigar com `UPDATE SET up_votes = up_votes + 1` atômico via JPQL
- (-) Se um `VoteEntity` for inserido sem incrementar o contador (bug), os valores divergem permanentemente

---

## Decision 3: Pergunta pode ser geral ou vinculada a um curso

**What:** O campo `course_id` em `CL_QUESTION` é nullable. Uma pergunta pode ser sobre um curso específico (`course_id NOT NULL`) ou uma discussão geral da comunidade (`course_id NULL`).

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Perguntas sempre vinculadas a um curso | Impede discussões gerais sobre programação ou a plataforma |
| Tabelas separadas para discussões gerais e perguntas de curso | Segmenta o conteúdo, mas duplica toda a estrutura de Q&A |

**Motivation:** Plataformas de educação têm valor tanto no conteúdo específico de cursos quanto nas discussões gerais da comunidade. Suportar ambos no mesmo modelo sem duplicar código é mais econômico.

**Consequences:**
- (+) Única API de Q&A serve tanto perguntas de curso quanto discussões gerais
- (+) Alunos podem pesquisar dúvidas por curso ou na comunidade geral
- (-) Listagem sem filtro retorna mix de perguntas gerais e de curso; UX precisa de separação visual ou filtros claros
- (-) Sem FK verificável para `course_id`; se um curso for arquivado, suas perguntas permanecem sem referência válida

---

## Decision 4: Votos são imutáveis após criação

**What:** Não existe endpoint para remover ou trocar um voto. Uma vez que `VoteEntity` é inserido, ele persiste. A unique constraint previne que o mesmo usuário vote duas vezes no mesmo alvo.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Toggle de voto (delete + possível insert de voto oposto) | Mais comum em plataformas modernas, mas adiciona complexidade de atualização do contador desnormalizado |
| Voto mutável (update do `vote_type`) | Permite mudança de posição, mas requer ajuste do contador (+1 e -1 na mesma operação atomicamente) |

**Motivation:** Simplifica o MVP. Votos imutáveis eliminam a lógica de decremento de contadores e possíveis race conditions em trocas de voto.

**Consequences:**
- (+) Lógica de voto simples: apenas INSERT + UPDATE do contador
- (+) Sem risco de contador negativo ou inconsistência no toggle
- (-) Usuário que votou errado não tem como corrigir
- (-) A imutabilidade é enforçada pela unique constraint; ausência de endpoint de delete é a única proteção

---

## Decision 5: `answers_count` e `has_accepted_answer` desnormalizados na pergunta

**What:** `QuestionEntity` armazena `answers_count` e `has_accepted_answer`. Esses campos permitem saber rapidamente se uma pergunta foi respondida e resolvida sem consultar `CL_ANSWER`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Calcular via JOIN/COUNT em cada leitura | Adiciona complexidade e custo nas queries de listagem |
| Sem campo `has_accepted_answer` | Exige subquery por pergunta em listagens para indicar status de resolução |

**Motivation:** Listagens de perguntas precisam indicar visualmente quais já têm resposta aceita ("resolvidas"). Sem desnormalização, isso exigiria um subquery ou JOIN para cada pergunta exibida.

**Consequences:**
- (+) Filtros e ordenações por status de resolução são eficientes
- (-) `answers_count` precisa ser incrementado ao criar e decrementado ao deletar uma resposta; se esses updates forem omitidos, o contador diverge
- (-) O módulo community gerencia dois contadores que dependem de lógica cross-entity
