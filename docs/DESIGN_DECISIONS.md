# Code Level - Decisões de Modelagem

## 📋 Índice
1. [Princípios Gerais](#princípios-gerais)
2. [Decisões Técnicas Críticas](#decisões-técnicas-críticas)
3. [Módulos e Justificativas](#módulos-e-justificativas)
4. [Performance e Escalabilidade](#performance-e-escalabilidade)
5. [Pontos de Atenção](#pontos-de-atenção)

---

## 🎯 Princípios Gerais

### 1. **Normalização vs Denormalização**
**Decisão**: Modelo majoritariamente normalizado (3FN) com **denormalização estratégica** para performance.

**Por quê?**
- Normalização garante integridade e evita anomalias
- Denormalização em campos calculados frequentemente (`total_enrollments`, `average_rating`, `answers_count`) evita JOINs pesados
- Trade-off consciente: mais complexidade em writes, menos em reads

**Onde denormalizamos:**
- `courses.total_lessons` → calculado via trigger
- `courses.total_enrollments` → atualizado via trigger
- `courses.average_rating` → calculado de `course_reviews`
- `questions.answers_count` → contagem de respostas
- `course_requests.upvotes/downvotes` → contagem de votos

**Por que não usar JOINs/COUNT em tempo real?**
- Listings de cursos (homepage, busca) seriam **extremamente lentos**
- Um `COUNT(*)` em milhões de matrículas é inaceitável
- Triggers garantem consistência sem overhead no read

---

### 2. **UUIDs vs Auto-increment IDs**
**Decisão**: 
- **UUID (v4)** para entidades principais (users, courses, lessons, etc)
- **SERIAL** para tabelas de lookup (categories, tags, levels, achievements)

**Por quê?**
- **UUIDs** evitam enumeration attacks e facilitam merge de dados (se tiver múltiplas instâncias no futuro)
- **SERIAL** para lookups economiza espaço e é mais legível (não há risco de exposição)
- UUIDs ocupam mais espaço (16 bytes vs 4 bytes), mas ganho de segurança compensa

**Quando usar cada um:**
```sql
-- UUID: entidades que podem ser expostas via API
users, courses, lessons, enrollments, etc

-- SERIAL: tabelas de lookup sem exposição direta
levels, categories, tags, achievements
```

---

### 3. **ENUM vs VARCHAR vs Tabela Separada**
**Decisão**: **ENUMs** para estados/tipos com valores fixos e pequenos.

**Por quê?**
- ENUMs garantem integridade sem FK (mais rápido)
- Ideal para valores que **raramente mudam** (roles, status, tipos)
- Se precisar mudar, ALTER TYPE é simples (não requer migração de dados)

**Quando evitar ENUMs:**
- Valores que **usuários** podem criar (categorias, tags) → tabela separada
- Valores que mudam frequentemente → tabela separada

---

### 4. **Soft Delete vs Hard Delete**
**Decisão**: **Hard delete** com CASCADE + status `ARCHIVED` para cursos.

**Por quê?**
- Simplicidade: não precisa filtrar `deleted_at IS NULL` em toda query
- Cursos usam `status = ARCHIVED` (mantém histórico sem afetar buscas)
- GDPR compliance: hard delete necessário para dados pessoais
- Auditoria: use tabela separada de `audit_log` (fora do escopo inicial)

---

## 🔧 Decisões Técnicas Críticas

### 1. **Tracking de Progresso de Vídeo**

#### **Decisão Híbrida**
```sql
lesson_progress {
    watch_time_seconds INTEGER      -- rastreado via API do player
    video_duration_seconds INTEGER  -- duração total
    completion_percentage DECIMAL   -- calculado
    is_completed BOOLEAN           -- trigger quando >= 90%
}
```

**Por quê híbrido?**
- **Ideal**: Rastrear via YouTube/Vimeo API
- **Fallback**: Botão "marcar como concluída" se API falhar
- **Validação**: Botão só habilitado após 90% do tempo mínimo (evita fraude)

**Implementação sugerida (Frontend):**
```typescript
// React: rastrear progresso via YouTube API
const handleVideoProgress = (currentTime: number, duration: number) => {
  const percentage = (currentTime / duration) * 100;
  
  // Atualizar backend a cada 10 segundos
  if (percentage >= 90 && !isCompleted) {
    markLessonAsCompleted();
  }
}
```

---

### 2. **Sistema de XP e Níveis**

#### **XP Fixo com Configurabilidade Futura**
```sql
lessons {
    xp_reward INTEGER DEFAULT 50  -- fixo inicialmente
}

exercises {
    xp_reward INTEGER DEFAULT 100  -- base (reduz 10% por erro)
}

xp_transactions {
    xp_amount INTEGER  -- histórico auditável
}
```

**Por quê?**
- Começar simples (XP fixo) e evoluir conforme necessidade
- `xp_transactions` permite auditoria completa
- Tabela `levels` já é configurável (admin pode ajustar thresholds)

**Cálculo de XP em exercícios:**
```sql
-- Exemplo: exercício vale 100 XP
Tentativa 1 (erro): 0 XP
Tentativa 2 (acerto): 90 XP (100 - 10%)
Tentativa 3 (acerto): 80 XP (100 - 20%)
```

**Trigger automático:**
```sql
CREATE TRIGGER create_xp_on_lesson_complete
AFTER INSERT OR UPDATE ON lesson_progress
FOR EACH ROW EXECUTE FUNCTION create_xp_transaction_on_lesson_complete();
```

Garante que XP é creditado automaticamente quando `is_completed = TRUE`.

---

### 3. **Avaliações: Curso vs Aula**

#### **Sistema Dual**
```sql
-- Avaliação obrigatória (rating 1-5 estrelas)
course_reviews {
    rating INTEGER CHECK (1-5)
    is_positive BOOLEAN GENERATED  -- auto: rating >= 4
}

-- Feedback opcional por aula (like/dislike + comentário)
lesson_feedbacks {
    is_helpful BOOLEAN
    comment TEXT
}
```

**Por quê separar?**
- **Rating geral** → decisão de "vale a pena?"
- **Feedback granular** → melhoria contínua do instrutor
- Evita fragmentação (curso com 50 aulas não precisa de 50 ratings)

**Agregação para aprovação de curso:**
```sql
-- Curso aprovado quando:
SELECT 
    (COUNT(*) FILTER (WHERE is_positive = TRUE)::DECIMAL / COUNT(*)) * 100 as approval_rate
FROM course_reviews
WHERE course_id = ?
HAVING approval_rate >= courses.approval_threshold;
```

---

### 4. **Solicitação de Cursos - Critérios de Aprovação**

#### **Threshold Ajustável**
```sql
course_requests {
    upvotes INTEGER
    downvotes INTEGER
    approval_percentage DECIMAL GENERATED  -- calculado automaticamente
}
```

**Regra implementada:**
```
Mínimo de votos: 20
Aprovação: 70% dos votos devem ser positivos

Fórmula: (upvotes / (upvotes + downvotes)) * 100 >= 70
```

**Por que GENERATED COLUMN?**
- Cálculo sempre correto (impossível ficar desatualizado)
- Performance: índice pode ser criado sobre coluna calculada
- Simplicidade: não precisa de trigger

**Query para aprovar automaticamente:**
```sql
UPDATE course_requests
SET status = 'APPROVED', approved_at = NOW()
WHERE status = 'PENDING'
  AND (upvotes + downvotes) >= 20  -- mínimo de votos
  AND approval_percentage >= 70;   -- % de aprovação
```

---

## 📦 Módulos e Justificativas

### 1. **Autenticação e Usuários**

```sql
users → levels (1:N)
users → social_media_accounts (1:N)
users → user_streaks (1:1)
```

**Decisões:**
- `role` como ENUM (ADMIN/INSTRUCTOR/STUDENT) → simplifica RBAC
- `level_id` calculado automaticamente via trigger baseado em `xp_total`
- `user_streaks` separado (1:1) → evita NULL em tabela principal

**Por que não RBAC completo (roles + permissions)?**
- Overkill para MVP
- 3 roles fixos são suficientes inicialmente
- Fácil evoluir depois (adicionar tabela `permissions` se necessário)

---

### 2. **Cursos e Conteúdo**

```sql
courses → modules → lessons → exercises
courses ←→ categories (N:N)
courses ←→ tags (N:N)
```

**Decisões:**
- **Módulos obrigatórios** → organização hierárquica clara
- **Exercícios 1:1 com aulas** → simplifica (aula pode ter 0 ou 1 exercício)
- **N:N para categories/tags** → flexibilidade (curso pode ter múltiplas categorias)

**Por que módulos são obrigatórios?**
```
Curso → Módulos → Aulas
```
- Melhor UX (usuário vê estrutura organizada)
- Facilita navegação e progresso
- Se curso for pequeno, crie 1 módulo "Conteúdo Principal"

---

### 3. **Progresso do Aluno**

```sql
course_enrollments → controla matrícula geral
lesson_progress → rastreia cada aula individualmente
exercise_attempts → histórico de tentativas (múltiplas)
```

**Decisões:**
- **3 tabelas separadas** → evita JSON gigante, permite queries eficientes
- `completion_percentage` calculado dinamicamente
- `exercise_attempts` guarda **todas** as tentativas (auditoria + estatísticas)

**Por que não consolidar tudo em 1 tabela?**
```sql
-- ❌ RUIM: tudo em JSON
user_course_data {
    progress JSONB  -- {lessons: [...], exercises: [...]}
}

-- ✅ BOM: tabelas separadas
lesson_progress + exercise_attempts
```

Motivos:
- Queries complexas ficam impossíveis em JSON
- Índices não funcionam bem em JSONB
- Relatórios/dashboards precisam de agregações

---

### 4. **Gamificação**

```sql
xp_transactions → histórico completo de XP
achievements → conquistas disponíveis
user_achievements → desbloqueios
user_streaks → dias consecutivos
```

**Decisões:**
- `xp_transactions` **imutável** (append-only) → auditoria completa
- `achievements` com `trigger_criteria` em JSONB → flexibilidade para regras customizadas
- `user_streaks` separado → cálculo diário via job/scheduler

**Exemplo de trigger_criteria:**
```json
{
  "type": "course_completed",
  "courses_completed": 1
}

{
  "type": "streak_days",
  "streak_days": 5
}

{
  "type": "lessons_completed",
  "lessons_completed": 10
}
```

**Como verificar conquistas automaticamente:**
```sql
-- Job rodando diariamente ou após cada evento relevante
SELECT a.id, a.name
FROM achievements a
WHERE NOT EXISTS (
    SELECT 1 FROM user_achievements ua
    WHERE ua.user_id = ? AND ua.achievement_id = a.id
)
AND (
    -- Verificar criteria dinamicamente
    (a.trigger_criteria->>'courses_completed')::INT <= (
        SELECT COUNT(*) FROM course_enrollments WHERE user_id = ? AND completed_at IS NOT NULL
    )
);
```

---

### 5. **Comunidade (Q&A)**

```sql
questions → answers → votes
```

**Decisões:**
- `votes` polimórfico → vota em pergunta OU resposta
- `upvotes` denormalizado → performance (ordenar por popularidade)
- `has_accepted_answer` na pergunta → feature "melhor resposta"

**Por que polimorfismo em `votes`?**
```sql
votes {
    votable_type ENUM ('QUESTION', 'ANSWER')
    votable_id UUID  -- pode ser ID de question ou answer
}
```

Alternativa (não escolhida):
```sql
-- ❌ Criar 2 tabelas separadas
question_votes
answer_votes
```

**Por que polimorfismo é melhor aqui:**
- Lógica de votação é idêntica (não precisa duplicar código)
- Queries de "meus votos" ficam simples
- Constraint `UNIQUE (user_id, votable_type, votable_id)` garante 1 voto por item

---

### 6. **Trilhas de Aprendizado**

```sql
learning_paths → learning_path_courses → courses
```

**Decisões:**
- **Admin-only** na criação (campo `creator_id`)
- `order_position` obrigatório → sequência definida
- `prerequisites` em texto livre → não bloqueia acesso (apenas informativo)

**Por que não bloquear pré-requisitos?**
```sql
-- ❌ Não escolhido: hard requirement
learning_path_courses {
    required_previous_course_id UUID  -- deve completar antes
}

-- ✅ Escolhido: soft requirement
learning_paths {
    prerequisites TEXT  -- "É recomendado conhecer JavaScript básico"
}
```

Motivos:
- Open-source → não queremos bloquear aprendizado
- Usuário pode já ter conhecimento prévio de outra fonte
- UX melhor (informar vs bloquear)

---

## 🚀 Performance e Escalabilidade

### 1. **Índices Estratégicos**

#### **Queries mais comuns:**
```sql
-- 1. Listar cursos (homepage)
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_published_at ON courses(published_at DESC);
CREATE INDEX idx_courses_average_rating ON courses(average_rating DESC NULLS LAST);

-- 2. Progresso do aluno
CREATE INDEX idx_enrollments_user_id ON course_enrollments(user_id);
CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);

-- 3. Comunidade (perguntas populares)
CREATE INDEX idx_questions_upvotes ON questions(upvotes DESC);
CREATE INDEX idx_questions_created_at ON questions(created_at DESC);
```

**Princípio:**
- Índice em FKs → JOINs mais rápidos
- Índice em campos de filtro (`status`, `is_completed`)
- Índice em campos de ordenação (`created_at DESC`, `upvotes DESC`)

---

### 2. **Particionamento (Futuro)**

**Quando considerar:**
```sql
-- Tabelas que crescem rapidamente:
xp_transactions → milhões de linhas
lesson_progress → milhões de linhas
exercise_attempts → milhões de linhas
```

**Estratégia sugerida:**
```sql
-- Particionamento por RANGE (data)
CREATE TABLE xp_transactions_2024_01 PARTITION OF xp_transactions
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE xp_transactions_2024_02 PARTITION OF xp_transactions
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

**Benefícios:**
- Queries filtradas por data ficam muito mais rápidas
- Manutenção simplificada (dropar partições antigas)
- Backup/restore mais granular

---

### 3. **Caching Strategy**

**Candidatos a cache (Redis/Memcached):**
```
1. Níveis (levels) → raramente mudam
2. Categorias (course_categories) → raramente mudam
3. Tags (course_tags) → raramente mudam
4. Cursos populares → TTL de 5min
5. Estatísticas de usuário → TTL de 1min
```

**Invalidação:**
```typescript
// Exemplo: invalidar cache quando curso é atualizado
async updateCourse(courseId: string, data: UpdateCourseDto) {
  await this.courseRepo.update(courseId, data);
  await this.cache.del(`course:${courseId}`);
  await this.cache.del(`popular_courses`); // se afetou popularidade
}
```

---

## ⚠️ Pontos de Atenção

### 1. **Race Conditions**

#### **Problema: Votos simultâneos**
```sql
-- Thread 1 e Thread 2 leem ao mesmo tempo
SELECT upvotes FROM course_requests WHERE id = ?; -- retorna 10

-- Ambas incrementam
UPDATE course_requests SET upvotes = 11 WHERE id = ?; -- ❌ RUIM
```

**Solução: Usar triggers atômicos**
```sql
-- Trigger já implementado atualiza automaticamente
INSERT INTO course_request_votes (course_request_id, user_id, vote_type)
VALUES (?, ?, 'UPVOTE');
-- Trigger incrementa de forma atômica: upvotes = upvotes + 1
```

---

### 2. **N+1 Queries**

#### **Problema comum:**
```typescript
// ❌ RUIM: N+1 query
const courses = await courseRepo.find();
for (const course of courses) {
  const instructor = await userRepo.findOne(course.instructor_id); // N queries!
}
```

**Solução: Eager loading**
```typescript
// ✅ BOM: 1 query com JOIN
const courses = await courseRepo.find({
  relations: ['instructor', 'categories', 'tags']
});
```

```sql
-- SQL gerado:
SELECT c.*, u.*, cat.*, tag.*
FROM courses c
LEFT JOIN users u ON c.instructor_id = u.id
LEFT JOIN course_category_mapping ccm ON c.id = ccm.course_id
LEFT JOIN course_categories cat ON ccm.category_id = cat.id
LEFT JOIN course_tag_mapping ctm ON c.id = ctm.course_id
LEFT JOIN course_tags tag ON ctm.tag_id = tag.id;
```

---

### 3. **Cascading Deletes**

**Atenção com ON DELETE CASCADE:**
```sql
-- ✅ BOM: progresso do aluno deletado se aluno deletar conta
lesson_progress → user (ON DELETE CASCADE)

-- ⚠️ CUIDADO: curso não pode ser deletado se tiver matrículas
courses → enrollments (ON DELETE RESTRICT)
```

**Por quê?**
- Deletar curso com milhares de alunos matriculados = perda de dados crítica
- Solução: usar `status = ARCHIVED` em vez de DELETE

---

### 4. **Transações Críticas**

**Operações que DEVEM ser transacionais:**
```typescript
// Completar curso
await db.transaction(async (trx) => {
  // 1. Marcar progresso como completo
  await trx('course_enrollments')
    .where({ user_id, course_id })
    .update({ completed_at: new Date(), progress_percentage: 100 });
  
  // 2. Criar transação de XP
  await trx('xp_transactions').insert({
    user_id,
    xp_amount: 500,
    source: 'COURSE_COMPLETED',
    source_id: course_id
  });
  
  // 3. Atualizar XP total
  await trx('users')
    .where({ id: user_id })
    .increment('xp_total', 500);
  
  // 4. Verificar conquistas
  await checkAchievements(trx, user_id);
  
  // 5. Gerar certificado
  await trx('certificates').insert({
    user_id,
    course_id,
    certificate_code: generateCode()
  });
});
```

**Por quê transação?**
- Se qualquer etapa falhar, rollback completo
- Evita estado inconsistente (ex: XP creditado mas certificado não gerado)

---

## 📊 Queries Complexas Úteis

### 1. **Top Instrutores**
```sql
SELECT 
    u.id,
    u.full_name,
    COUNT(DISTINCT c.id) as courses_created,
    COUNT(DISTINCT ce.id) as total_students,
    AVG(cr.rating) as average_rating,
    SUM(c.total_enrollments) as total_impact
FROM users u
JOIN courses c ON u.id = c.instructor_id
LEFT JOIN course_enrollments ce ON c.id = ce.course_id
LEFT JOIN course_reviews cr ON c.id = cr.course_id
WHERE u.role = 'INSTRUCTOR'
  AND c.status IN ('COMMUNITY_APPROVED', 'EXPERT_APPROVED')
GROUP BY u.id
ORDER BY total_impact DESC
LIMIT 10;
```

---

### 2. **Cursos Próximos da Aprovação**
```sql
WITH course_feedback_stats AS (
    SELECT 
        c.id,
        c.title,
        c.approval_threshold,
        COUNT(*) FILTER (WHERE cr.is_positive = TRUE) as positive_count,
        COUNT(*) as total_count,
        (COUNT(*) FILTER (WHERE cr.is_positive = TRUE)::DECIMAL / COUNT(*)) * 100 as current_percentage
    FROM courses c
    JOIN course_reviews cr ON c.id = cr.course_id
    WHERE c.status = 'EXPERIMENTAL'
    GROUP BY c.id
)
SELECT *
FROM course_feedback_stats
WHERE current_percentage >= (approval_threshold - 5)  -- próximo de atingir
  AND current_percentage < approval_threshold
ORDER BY current_percentage DESC;
```

---

### 3. **Alunos em Risco de Desistência**
```sql
-- Alunos que não estudam há mais de 7 dias
SELECT 
    u.id,
    u.full_name,
    u.email,
    MAX(lp.last_watched_at) as last_activity,
    CURRENT_DATE - MAX(lp.last_watched_at)::DATE as days_inactive
FROM users u
JOIN lesson_progress lp ON u.id = lp.user_id
WHERE u.role = 'STUDENT'
GROUP BY u.id
HAVING CURRENT_DATE - MAX(lp.last_watched_at)::DATE >= 7
ORDER BY days_inactive DESC;
```

---

## 🎓 Conclusão

Esta modelagem foi projetada com os seguintes princípios:

1. **Escalabilidade**: Denormalização estratégica + índices adequados
2. **Integridade**: Constraints, triggers e transações garantem consistência
3. **Flexibilidade**: ENUMs e JSON permitem evolução sem migrações pesadas
4. **Performance**: Campos calculados, views e índices otimizados
5. **Simplicidade**: Começar simples (XP fixo, 3 roles) e evoluir conforme necessidade

**Próximos passos:**
- Implementar seeders com dados de exemplo
- Criar testes de carga (simular 10k alunos, 1k cursos)
- Monitorar queries lentas (pg_stat_statements)
- Implementar cache em queries críticas

---

**Claudin - Mentor de Arquitetura de Software** 🚀
