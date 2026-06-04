# ADR-014: Módulo de Integration

## Status
Accepted

## Context

O CodeLevel precisa publicar automaticamente conteúdo em redes sociais (LinkedIn e Instagram) para divulgar marcos de usuários, novos cursos aprovados e atualizações da plataforma. Cada plataforma tem uma API distinta; durante desenvolvimento e staging, chamadas reais às APIs externas devem ser evitadas; posts podem falhar por instabilidade externa e precisam ser retentáveis.

---

## Decision 1: Factory pattern para abstrair publishers

**What:** O módulo expõe uma interface `SocialMediaPublisher` com um único método `publish(SocialMediaPostEntity)`. Cada plataforma implementa essa interface (`LinkedInPublisher`, `InstagramPublisher`). Um `SocialMediaPublisherFactory` centraliza a seleção da implementação correta em runtime.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| `if/switch` diretamente no serviço | Acopla o serviço aos detalhes de cada plataforma; cresce a cada nova plataforma adicionada |
| `@Named` injection via CDI | Elegante no container, mas torna o toggle mock/real mais complexo |

**Motivation:** A factory encapsula tanto a seleção por plataforma quanto o toggle mock/real em um único ponto. Adicionar uma nova plataforma requer apenas uma nova implementação de `SocialMediaPublisher`.

**Consequences:**
- (+) `SocialMediaPostService` não conhece detalhes de nenhuma plataforma
- (+) Ponto único para adicionar novas plataformas
- (-) A factory é um indirection extra; stack trace passa pela factory antes de chegar ao publisher real

---

## Decision 2: Toggle mock/real via configuração, não por perfil de build

**What:** A factory lê `integration.mock-publishing` (boolean, default `true`). Quando `true`, todos os posts são roteados para `MockSocialMediaPublisher`, que apenas loga a ação sem fazer chamadas HTTP.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Perfis de build Maven (`-Pdev`, `-Pprod`) | Requer redeploy para mudar o comportamento; não permite toggle em produção sem rebuild |
| Stub HTTP via WireMock nos testes | Correto para testes, mas não resolve o cenário de staging com API externa indisponível |

**Motivation:** Em staging, a equipe precisa testar o fluxo completo sem afetar contas reais nas redes sociais. Uma flag de configuração permite isso sem rebuild.

**Consequences:**
- (+) Ambientes de staging com comportamento fiel ao prod sem efeitos colaterais externos
- (+) Possibilidade de ativar mock temporariamente em produção durante manutenção das APIs externas
- (-) Um erro de configuração (`mock-publishing=true` em prod) silencia falhas reais de publicação; requer monitoramento de alertas

---

## Decision 3: Processo em duas fases para Instagram

**What:** A publicação no Instagram requer duas chamadas sequenciais à Graph API: (1) criação do container de mídia, que retorna um `creation_id`; (2) publicação do container via `media_publish`. O `InstagramPublisher` executa ambas e lança exceção se qualquer uma falhar.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Tentativa de publicar diretamente (1 chamada) | A API do Instagram não suporta publicação direta de posts com mídia em uma única requisição para contas Business/Creator |
| Armazenar o `creation_id` entre as fases | Adicionaria estado intermediário com mais uma coluna na tabela e lógica de recuperação de falha |

**Motivation:** O processo em duas fases é uma limitação da API. Encapsular ambas as chamadas em um único `publish()` mantém a interface do publisher simples.

**Consequences:**
- (+) Interface uniforme com LinkedIn (`publish()` único)
- (+) Retry recomeça desde a primeira fase, garantindo consistência
- (-) Se a primeira fase criar o container com sucesso mas a segunda falhar, o container ficará pendente na conta Instagram até expirar (~24h); sem limpeza automática

---

## Decision 4: Máquina de estados do post: PENDING → POSTED | FAILED

**What:** Todo post criado nasce com status `PENDING`. Ao chamar `publishPost()`, o status transiciona para `POSTED` (sucesso) ou `FAILED` (exceção). Posts com status `FAILED` podem ser retentados via `retryPost()`.

Transições válidas:
```
PENDING  → POSTED  (via publishPost)
PENDING  → FAILED  (via publishPost, exceção)
FAILED   → POSTED  (via retryPost)
FAILED   → FAILED  (via retryPost, nova exceção)
```

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Publicar no momento da criação | Não permite agendar publicações futuras nem revisar conteúdo antes de publicar |
| Estado adicional `SCHEDULED` | Campo `scheduledAt` modelado na entidade, mas lógica de job não implementada neste ciclo |

**Motivation:** Separar criação de publicação permite revisão manual. O status `FAILED` com `error_message` facilita diagnóstico sem depender de logs.

**Consequences:**
- (+) Posts podem ser revisados antes de publicar
- (+) Diagnóstico de falha visível na entidade
- (-) Posts com status `PENDING` não são publicados automaticamente; requer ação manual do admin
- (-) A feature de agendamento (`scheduledAt`) está modelada no banco mas sem implementação de job

---

## Decision 5: Credenciais das APIs externas via variáveis de ambiente

**What:** Os tokens de acesso e identificadores de conta são lidos de variáveis de ambiente (`LINKEDIN_ACCESS_TOKEN`, `LINKEDIN_AUTHOR_URN`, `INSTAGRAM_ACCESS_TOKEN`, `INSTAGRAM_USER_ID`) via `@ConfigProperty`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Credenciais no banco de dados | Adiciona complexidade de criptografia em repouso e exposição via API se mal protegido |
| Credenciais em `application.yml` versionado | Viola a regra de não versionar segredos |
| Vault/secrets manager | Ideal para produção em escala, mas dependência de infraestrutura fora do escopo do MVP |

**Motivation:** Variáveis de ambiente são o padrão da metodologia 12-Factor App. Compatível com todos os provedores de cloud sem dependências adicionais.

**Consequences:**
- (+) Segredos nunca entram no repositório Git
- (+) Compatível com Docker, Kubernetes, e serviços de PaaS sem configuração extra
- (-) Tokens têm prazo de validade e precisam de processo de renovação manual periódica

---

## Decision 6: LinkedIn suporta apenas texto (sem mídia)

**What:** O `LinkedInPublisher` usa a UGC API v2 do LinkedIn e publica apenas posts de texto. O campo `media_url` existe na entidade mas é ignorado pela implementação atual.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Suporte a upload de imagem via Assets API | Fluxo requer passo adicional de upload antes da publicação; complexidade sem benefício imediato para os tipos de post do MVP |

**Motivation:** Texto sufice para os tipos de post planejados no MVP. Implementar upload de assets adicionaria código significativo sem uso imediato.

**Consequences:**
- (+) Implementação simples, menor surface de falha
- (-) Posts no LinkedIn sem imagem têm menor engajamento; limitação conhecida para o MVP
- (-) O campo `media_url` é aceito pelo sistema mas ignorado ao publicar no LinkedIn; sem aviso explícito ao caller

---

## Decision 7: Acesso restrito a ADMIN e INSTRUCTOR

**What:** Endpoints de leitura requerem `ROLE_ADMIN` ou `ROLE_INSTRUCTOR`. Endpoints de escrita e ação requerem apenas `ROLE_ADMIN`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Acesso apenas para ADMIN em todos os endpoints | Impede que instrutores acompanhem posts sobre seus próprios cursos |
| Instrutores criam posts sobre seus próprios cursos | Exigiria lógica de autorização baseada em `referenceId`, aumentando complexidade |

**Motivation:** Instrutores têm interesse legítimo em ver o status de posts de seus cursos sem poder criar ou publicar. Criar e publicar posts é responsabilidade editorial do time de ADMIN.

**Consequences:**
- (+) Separação clara entre visualização (instrutor) e ação editorial (admin)
- (-) Instrutores veem posts de outros instrutores também, sem filtragem por `referenceId`
