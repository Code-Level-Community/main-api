# ADR-013: Módulo de Gamification

## Status
Accepted

## Context

O sistema CodeLevel precisa de um mecanismo de engajamento que incentive alunos a estudar continuamente, completar exercícios e voltar diariamente à plataforma. As principais necessidades identificadas foram: recompensar ações com pontos de experiência (XP) de forma auditável, progredir entre níveis, reconhecer marcos com achievements e rastrear consistência diária via streaks.

O módulo deve funcionar de forma isolada, sem criar dependências cruzadas com `identity`, `course` ou `student_progress`.

---

## Decision 1: XP como transações append-only

**What:** O saldo de XP de um usuário nunca é armazenado explicitamente. Todo crédito ou débito é registrado como uma linha em `CL_XP_TRANSACTION`, e o total é calculado pela soma (`SUM`) dessas linhas em tempo de consulta.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Campo `xp_total` desnormalizado em `users` | Risco de divergência entre saldo e histórico; exigiria dependência cruzada (gamification modificando entidade de identity) |
| Saldo em tabela própria dentro de gamification | Ainda divide a fonte da verdade entre saldo e histórico |

**Motivation:** O histórico de transações é a fonte da verdade. Não há forma de o saldo ficar dessincronizado; toda ação é rastreável e qualquer ajuste futuro é feito inserindo uma nova transação.

**Consequences:**
- (+) Auditoria completa de todo XP ganho e gasto por usuário
- (+) Eliminação de race conditions em operações simultâneas de crédito
- (-) `getTotalXp()` executa um `SUM` a cada chamada; mitigar com cache se necessário em produção
- (-) Impossível corrigir uma transação errada diretamente; requer estorno

---

## Decision 2: Cap diário de XP com truncamento do valor

**What:** Cada usuário pode ganhar no máximo 500 XP por dia. Se o `award()` solicitado ultrapassaria o limite, o valor é truncado ao restante disponível. Se o restante for zero, a operação lança `BusinessRuleException`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Rejeitar qualquer award que individualmente exceda 500 XP | Penaliza usuários que chegam ao limite com prêmios parcialmente válidos |
| Sem cap | Permite acúmulo ilimitado; desmotiva usuários que já alcançaram um grande lead |

**Motivation:** Truncar preserva o máximo de valor para o usuário dentro do limite justo. O cap previne grinding excessivo e mantém o leaderboard competitivo.

**Consequences:**
- (+) Proteção contra farming de XP em sessões longas
- (+) Leaderboard mais equilibrado entre usuários casuais e intensivos
- (-) Usuários que estudam muito em um único dia percebem o cap; deve ser comunicado claramente na UI
- (-) A lógica de truncamento precisa ser aplicada após o cálculo do bônus de streak

---

## Decision 3: Bônus de streak aplicado antes do cap diário

**What:** Quando `currentStreakDays >= 7`, o valor do award é multiplicado por `1.5×` antes de verificar o cap diário. O cap incide sobre o valor já multiplicado.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Bônus aplicado após o cap | O usuário receberia `min(amount, cap) * 1.5`, podendo ultrapassar 500 XP/dia, invalidando o propósito do cap |
| Sem bônus de streak | Simplifica o cálculo, mas reduz o incentivo para manutenção de streaks longas |

**Motivation:** O bônus deve incentivar consistência, mas não contornar o mecanismo de equilíbrio. Aplicar o multiplicador antes do cap mantém a regra coerente: o limite diário é sempre 500 XP, independente de bônus.

**Consequences:**
- (+) Incentivo real para manter streaks longas
- (+) Regra de cap permanece previsível para o usuário
- (-) Com bônus de 1.5×, o usuário atinge o cap com 334 XP brutos em vez de 500; requer boa UX explicando o multiplicador

---

## Decision 4: Detecção de level-up no ato do award

**What:** O método `award()` retorna `AwardXpResult`, um record que inclui `leveledUp: boolean` e `currentLevel: Optional<LevelEntity>`. A detecção compara o nível antes e após adicionar a transação dentro da mesma chamada.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Detectar level-up em consulta separada pelo cliente | O caller teria que chamar `getTotalXp()` e `getCurrentLevel()` antes e depois do award, com risco de condições de corrida |
| Evento assíncrono de level-up | Ideal para sistemas distribuídos, mas adiciona complexidade desnecessária em um monólito |

**Motivation:** Encapsular a detecção no próprio award simplifica todos os callers que precisam exibir feedback imediato ao usuário após ganhar XP.

**Consequences:**
- (+) Interface limpa: uma chamada retorna tudo que a UI precisa para animação de level-up
- (+) Sem condição de corrida na detecção
- (-) `award()` executa duas queries de nível (antes e depois); custo aceitável dado que são queries por índice sobre tabela pequena

---

## Decision 5: Achievements com trigger numérico armazenado como String

**What:** O campo `trigger_criteria` em `CL_ACHIEVEMENT` é uma `VARCHAR` que armazena um número inteiro textual (e.g., `"10"`). Durante `checkAndUnlock()`, o critério é parseado como `long` e comparado com `currentValue`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Coluna `INTEGER` | Mais restritivo; impede evolução para critérios compostos no futuro |
| JSONB | Máxima flexibilidade, mas requer parsing elaborado e dificulta queries SQL diretas para o MVP |

**Motivation:** A MVP precisa apenas de thresholds numéricos simples. Armazenar como String permite evolução natural sem migration imediata; a validação no serviço garante integridade do formato.

**Consequences:**
- (+) Flexibilidade de schema sem overhead de JSONB
- (+) Validação em código garante integridade do formato
- (-) Sem constraint de banco para o formato da string; um insert direto poderia inserir um critério inválido

---

## Decision 6: Cascade unlock para meta-achievements

**What:** Após qualquer unlock de achievement, o serviço automaticamente re-executa `checkAndUnlock(userId, ACHIEVEMENT_UNLOCKED, totalAchievements)` para avaliar achievements do tipo "desbloqueie N achievements".

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Caller responsável por checar cascade | Exige que todo ponto de código que desbloqueia achievements acione o cascade manualmente; sistema frágil |
| Trigger de banco | Automático, mas lógica de negócio no banco é difícil de testar e evolutiva |

**Motivation:** Centralizar o cascade em `AchievementService.unlock()` garante que nenhum unlock escape da verificação, independentemente de quem chamou o desbloqueio.

**Consequences:**
- (+) Cobertura completa do cascade sem duplicação de lógica
- (-) Uma única ação pode desencadear múltiplos unlocks em cadeia; necessário garantir que achievements ACHIEVEMENT_UNLOCKED não referenciem a si mesmos para evitar recursão infinita

---

## Decision 7: Freeze de streak coordenado pelo StreakResource

**What:** O `POST /gamification/streaks/freeze` é tratado inteiramente no `StreakResource`, que chama `xpService.spend()` (débito de 100 XP) e depois `streakService.activateFreeze()` em sequência dentro de uma única transação `@Transactional`. O `StreakService` não injeta `XpService`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| `StreakService.freezeStreak()` injeta `XpService` | Criaria dependência circular (`XpService` já consulta `StreakService` para calcular bônus de streak) |
| `Instance<XpService>` lazy injection | Resolve o ciclo no container IoC, mas oculta a dependência circular em vez de eliminá-la |

**Motivation:** O resource é o único ponto onde a coordenação deve ocorrer. Mover a orquestração para fora dos serviços elimina o ciclo sem subterfúgios de injeção lazy.

**Consequences:**
- (+) Serviços com responsabilidades claras e sem ciclos de dependência
- (+) Testabilidade: cada serviço pode ser testado isoladamente
- (-) O resource contém lógica de coordenação além de I/O HTTP; aceitável dado que é uma operação composta de granularidade fina e não replicada em outros resources

---

## Decision 8: UserStreakEntity com userId como PK

**What:** A tabela `CL_USER_STREAK` usa `us_user_id` como chave primária. Há exatamente um registro de streak por usuário.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Sequência própria como PK com unique constraint em `user_id` | Mais flexível para múltiplos streaks por tipo de atividade, mas complexidade desnecessária para o modelo atual |

**Motivation:** Streak é uma propriedade singleton do usuário. Usar `userId` como PK elimina a necessidade de `findByUserId()` para a operação mais comum, tornando o `persist()` direto um upsert natural.

**Consequences:**
- (+) Queries de streak são lookups por PK (O(1) com índice)
- (-) Se futuramente for necessário rastrear streaks por tipo de atividade, será necessária migração da PK

---

## Decision 9: Levels como configuração administrativa

**What:** A tabela `CL_LEVEL` não tem registros pré-populados na migration. Admins criam os níveis via `POST /gamification/levels`. O sistema funciona sem níveis configurados (`currentLevel` retorna `Optional.empty()`).

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Níveis fixos no código (enums) | Sem flexibilidade para ajuste de thresholds sem redeploy |
| Níveis pré-inseridos na migration | Amarra o produto a thresholds definidos antes de dados reais de uso |

**Motivation:** Thresholds de XP ideais dependem de dados reais de uso. Deixar a configuração como responsabilidade do admin permite ajuste contínuo sem código novo.

**Consequences:**
- (+) Flexibilidade total para balancear a progressão com dados reais
- (+) Sistema tolerante a ausência de configuração
- (-) Sem nenhum nível configurado, a feature de progressão não aparece na UI; requer disciplina operacional para configurar antes do lançamento
