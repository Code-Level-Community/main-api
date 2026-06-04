# ADR-015: Módulo de Identity

## Status
Accepted

## Context

O módulo `identity` é o núcleo de segurança da plataforma: gerencia cadastro de usuários, autenticação, autorização e sessões. As decisões de design aqui impactam todos os outros módulos, pois o JWT emitido carrega as informações que outros serviços usam para identificar e autorizar requests.

---

## Decision 1: Refresh token como UUID simples armazenado em banco

**What:** O refresh token é um `UUID.randomUUID()` persistido na tabela `CL_REFRESH_TOKEN` com data de expiração de 7 dias. Não é um JWT assinado — é apenas um identificador opaco verificado por lookup no banco.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Refresh token como JWT assinado | Tornaria a revogação impossível sem uma blocklist — contradiz o objetivo de revogar no logout |
| JWT de refresh com blocklist | Adiciona complexidade de manutenção da blocklist sem ganho real em relação ao UUID em banco |

**Motivation:** O principal requisito do refresh token é ser revogável. UUID em banco é revogável por simples `DELETE`. Um JWT assinado com TTL longo seria impossível de invalidar sem infraestrutura adicional de blocklist.

**Consequences:**
- (+) Logout real: `DELETE FROM CL_REFRESH_TOKEN WHERE username = ?` garante que nenhum refresh token do usuário permaneça ativo
- (+) Rastreabilidade: a tabela registra data de criação e expiração de cada token
- (-) Cada `/auth/refresh` exige um roundtrip ao banco para validar o token
- (-) Tokens antigos do mesmo usuário são deletados no login (sessão única por design)

---

## Decision 2: Sessão única por usuário (single-session)

**What:** No `login()`, todos os refresh tokens existentes do usuário são deletados antes de criar o novo. Um usuário só pode ter uma sessão ativa por vez.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Multi-sessão (um token por dispositivo) | Permite login simultâneo em múltiplos dispositivos, mas exige associar tokens a dispositivos e lógica de revogação seletiva |
| Sem revogação no login | Tokens antigos expiram naturalmente após 7 dias; sessões antigas permanecem ativas até o TTL |

**Motivation:** Simplifica o modelo de segurança para o MVP. Se a conta é comprometida e o usuário faz novo login, todos os tokens anteriores são automaticamente invalidados.

**Consequences:**
- (+) Segurança simplificada: novo login protege automaticamente contra tokens roubados
- (+) Sem acumulação de tokens obsoletos no banco
- (-) Usuário perde sessão em outros dispositivos ao fazer login em um novo
- (-) Comportamento pode surpreender usuários que esperam multi-sessão

---

## Decision 3: @PrePersist atribui automaticamente ROLE_USER a todo novo usuário

**What:** O hook `@PrePersist` em `UserEntity` chama `RoleEntity.findByName("ROLE_USER")` e adiciona o papel à coleção de roles antes de persistir. Todo usuário criado nasce com `ROLE_USER` sem que o caller precise especificar.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Role atribuída pelo serviço | Explícita e visível no fluxo, mas duplicada em todo ponto que cria usuários (signup, seed, admin, etc.) |
| Role via migration de seed | Não resolve criação dinâmica de usuários |

**Motivation:** `ROLE_USER` é um invariante de qualquer conta ativa — não há caso onde um usuário legítimo não deva tê-la. Mover essa garantia para o `@PrePersist` elimina a possibilidade de esquecer de atribuí-la.

**Consequences:**
- (+) Invariante garantido na camada de persistência, impossível de ser esquecido
- (-) Lógica de negócio no hook de ORM; pode surpreender ao depurar
- (-) `@PrePersist` executa uma query ao banco (`findByName("ROLE_USER")`) — a role precisa existir antes do primeiro usuário ser criado (bootstrap via `UserEntityInitializer`)

---

## Decision 4: Login case-insensitive via LOWER() em JPQL

**What:** A query de busca por username usa `LOWER(username) = LOWER(?1)` em vez de comparação direta. O banco pode receber `"Admin"`, `"ADMIN"` ou `"admin"` e todos resolvem para o mesmo usuário.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Normalizar para minúsculas no cadastro | Exige que todos os pontos de criação de usuário normalizem antes de persistir |
| Case-sensitive | Usuário precisa lembrar o case exato do username que usou; UX ruim |

**Motivation:** Usernames são identificadores amigáveis para humanos. Diferenciar `"Lucas"` de `"lucas"` cria atrito desnecessário e erros de login difíceis de diagnosticar.

**Consequences:**
- (+) UX melhor no login
- (-) `LOWER()` na query impede uso do índice B-tree padrão em `username`; mitigar com índice funcional `CREATE INDEX idx_user_username_lower ON CL_USER(LOWER(username))` em produção

---

## Decision 5: Soft delete via campo `enabled`

**What:** Deletar um usuário seta `enabled = false` em vez de remover o registro. Queries de busca por usuário ativo filtram `enabled = true`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Hard delete com ON DELETE CASCADE | Destrói o histórico de XP, enrollments, reviews e outros dados associados ao usuário |
| Soft delete com campo `deleted_at` | Padrão mais comum, mas `enabled` boolean é semanticamente mais direto para conta ativa/inativa |

**Motivation:** O histórico de dados de um usuário tem valor para auditoria e estatísticas mesmo após o usuário "deletar" a conta. Soft delete preserva esse histórico e permite reativação.

**Consequences:**
- (+) Histórico de usuário preservado para auditoria
- (+) Possibilidade de reativar contas desativadas
- (-) Queries precisam filtrar `enabled = true`; risco de vazar dados de usuários desativados se o filtro for esquecido
- (-) Username e email de usuários desativados ainda ocupam os slots de unique constraint

---

## Decision 6: Fetch EAGER de roles e permissões

**What:** `UserEntity.roles` é `@ManyToMany(fetch = FetchType.EAGER)` e `RoleEntity.permissions` também é EAGER. Carregar qualquer `UserEntity` traz automaticamente todas as suas roles e permissões.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| LAZY fetch | Mais eficiente em queries que não precisam de autorização, mas risco de `LazyInitializationException` fora da sessão JPA |
| Cache de roles separado | Elimina queries repetidas, mas adiciona complexidade de invalidação a cada novo voto |

**Motivation:** O Quarkus Security JPA precisa das roles para popular os claims do JWT no login. Como toda autenticação acessa roles, carregar eagerly é economicamente neutro no caminho crítico. O risco de N+1 é mitigado pelo cache de usuário (ADR-009).

**Consequences:**
- (+) Sem `LazyInitializationException` no acesso a roles fora de sessão JPA
- (+) Security JPA funciona sem configuração adicional
- (-) Queries de listagem de usuários carregam roles/permissões de todos, mesmo quando não necessário
- (-) Com muitas permissões por role, o objeto `UserEntity` em memória pode ficar grande
