# Manual de Contribuição

Obrigado pelo interesse em contribuir com o **Code Level**! Este guia cobre todo o fluxo — do fork ao merge — seja para código ou conteúdo.

**Antes de começar:** leia o [Código de Conduta](CODE_OF_CONDUCT.md). Ao contribuir, você concorda em respeitá-lo.

---

## Índice

1. [Configuração do Ambiente](#1-configuração-do-ambiente)
2. [Fluxo de Trabalho de Código](#2-fluxo-de-trabalho-de-código)
3. [Convenções de Commit](#3-convenções-de-commit)
4. [Abrindo um Pull Request](#4-abrindo-um-pull-request)
5. [Processo de Code Review](#5-processo-de-code-review)
6. [Padrões de Código e Arquitetura](#6-padrões-de-código-e-arquitetura)
7. [Testes](#7-testes)
8. [Contribuição de Conteúdo](#8-contribuição-de-conteúdo)
9. [Reportar Vulnerabilidades](#9-reportar-vulnerabilidades)

---

## 1. Configuração do Ambiente

### Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|---|---|---|
| Java (JDK) | 17 | Testado com Temurin e GraalVM CE |
| Maven Wrapper | — | Incluído (`./mvnw`) |
| Docker | Qualquer recente | Opcional — apenas para banco externo |
| GraalVM | 22+ | Somente para build nativo |

### Setup inicial

```bash
# 1. Fork o repositório pelo GitHub e clone o seu fork
git clone https://github.com/<seu-usuario>/demo-quarkus-graalvm.git
cd demo-quarkus-graalvm

# 2. Configure o upstream para manter o fork atualizado
git remote add upstream https://github.com/codelevel/demo-quarkus-graalvm.git

# 3. Rode em modo dev (live reload + H2 in-memory)
./mvnw quarkus:dev
```

Dev UI disponível em `http://localhost:8080/q/dev/`. Todas as APIs têm prefixo `/api`.

### Sincronizando o fork antes de trabalhar

```bash
git fetch upstream
git checkout main
git merge upstream/main
```

---

## 2. Fluxo de Trabalho de Código

```
upstream/main ←── PR ←── fix/codelevel-123       (no seu fork)
                          feature/codelevel-124
                          improvement/codelevel-132
                          docs/codelevel-145
```

### Passo a passo

1. **Abra ou encontre uma Issue** que descreva o problema ou a feature antes de começar qualquer código.
2. **Sincronize o fork** com o upstream (veja acima).
3. **Crie uma branch** a partir do `main` seguindo a convenção abaixo.
4. **Implemente** a mudança com commits atômicos e bem descritos.
5. **Rode os testes** localmente (`./mvnw test`) e certifique-se que tudo passa.
6. **Abra um Pull Request** contra o `main` do repositório original com a descrição completa.

### Nomenclatura de branches

| Tipo | Padrão | Exemplo |
|---|---|---|
| Correção de bug | `fix/codelevel-[numero-issue]` | `fix/codelevel-123` |
| Nova feature | `feature/codelevel-[numero-issue]` | `feature/codelevel-124` |
| Refatoração/melhoria | `improvement/codelevel-[numero-issue]` | `improvement/codelevel-132` |
| Documentação | `docs/codelevel-[numero-issue]` | `docs/codelevel-145` |

> Branches de trabalho são removidas após o merge para manter o repositório limpo.

---

## 3. Convenções de Commit

Usamos [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
<tipo>(<escopo>): <mensagem em modo imperativo>

[corpo opcional — explique o PORQUÊ, não o QUÊ]

[rodapé opcional: closes #<issue>]
```

### Tipos permitidos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade visível ao usuário |
| `fix` | Correção de bug |
| `refactor` | Refatoração sem mudança de comportamento |
| `test` | Adicionar ou corrigir testes |
| `docs` | Atualização de documentação |
| `chore` | Configuração, build, dependências |
| `perf` | Melhoria de performance |

### Exemplos

```
feat(gamification): implementa XP cap diário de 500 pontos por usuário

closes #98
```

```
fix(certificate): corrige race condition no dispatch do Vert.x Event Bus

O commit ocorria antes do flush, tornando o registro invisível para o
consumidor assíncrono. Invertida a ordem: persistAndFlush() antes do
eventBus.send().

closes #112
```

```
test(identity): adiciona ServiceIT para registro com email duplicado
```

---

## 4. Abrindo um Pull Request

1. Certifique-se de que **todos os testes passam** localmente: `./mvnw test`.
2. Abra o PR contra o branch `main` do repositório original.
3. **Use o template de PR** (preenchido automaticamente ao abrir o PR) — não deixe seções em branco.
4. Vincule a Issue relacionada com `Closes #xxx` na descrição.
5. Aguarde pelo menos **1 aprovação** de um mantenedor antes do merge.

### Tamanho do PR

Prefira PRs focados. Um PR que adiciona um módulo completo deve ser dividido por camada (migration + entities → services → resources → testes). Um PR grande demora mais para ser revisado e tem maior chance de conflito.

### Quando não abrir um PR

- Sem Issue prévia para mudanças não triviais.
- Sem testes para nova lógica de negócio ou endpoints.
- Com violações dos padrões de arquitetura (veja seção 6).

---

## 5. Processo de Code Review

**Como autor:**
- Responda todos os comentários — mesmo que seja para discordar (com justificativa técnica).
- Adicione commits novos para endereçar feedback; evite force-push durante a review.
- Marque as threads como resolvidas após endereçar cada ponto.
- Se um comentário ficou sem resposta por mais de 3 dias, mencione o revisor.

**Como revisor:**
- Verifique o checklist de arquitetura (seção 6) antes de aprovar.
- Prefira comentários com sugestão de alternativa concreta.
- Use **Request changes** apenas para violações de arquitetura, bugs ou falta de testes.
- Comentários de estilo ou preferência pessoal são informativos — não bloqueiam.
- A meta é feedback construtivo, não perfeito.

---

## 6. Padrões de Código e Arquitetura

O projeto segue uma arquitetura de **monolito modular** com 9 módulos de domínio. Violações dos padrões abaixo são bloqueadas na review.

### Estrutura interna de um módulo

```
module/{nome}/
├── http/rest/
│   ├── routes/*Resource.java   ← JAX-RS controllers (@Path)
│   ├── dto/                    ← DTOs de request/response HTTP
│   ├── handler/                ← Exception mappers (@Provider)
│   └── mapper/                 ← Entity ↔ DTO mappers
├── persistence/
│   ├── resource/               ← Services (@ApplicationScoped @Transactional)
│   │   └── dto/                ← DTOs internos do serviço (records)
│   └── entity/                 ← Entidades JPA (extends PanacheEntityBase)
│       └── enums/
└── domain/                     ← Value objects e lógica de domínio pura
```

### Checklist de arquitetura

- [ ] Módulos se comunicam **exclusivamente via REST** — proibido `@Inject` cross-module
- [ ] Controllers são finos: delegam tudo para services, sem lógica de negócio
- [ ] Entidades JPA não são expostas na API — mapear para DTOs antes de retornar
- [ ] Query methods retornam `Optional<T>`, nunca `null`
- [ ] Domain objects validam no construtor (factory method `of()` com throw)
- [ ] Exceptions de domínio (`BusinessRuleException`, `ResourceNotFound`, `ResourceAlreadyExists`) — nunca `WebApplicationException`
- [ ] Constructor injection — nunca `@Inject` em campo com risco de null em testes
- [ ] Mudanças de schema têm migration Flyway em `src/main/resources/db/migration/V{n}__*.sql`
- [ ] Sem reflection dinâmica sem registro (compatibilidade com GraalVM native image)

> Para exemplos detalhados com código, veja o guia em `CLAUDE.md`.

---

## 7. Testes

O projeto adota três tipos de teste distintos (conforme [ADR-022](docs/adr/ADR-022-test-strategy.md)):

| Tipo | Sufixo | Container Quarkus | Banco | O que valida |
|---|---|---|---|---|
| Unitário | `*Test` | Não | Não | Value objects, invariantes de domínio |
| Service IT | `*ServiceIT` | Sim | H2 in-memory | Regras de negócio, queries Panache, transações |
| Resource IT | `*ResourceIT` | Sim | H2 in-memory | Contrato HTTP, status codes, RBAC, JSON |

### Rodando testes

```bash
# Todos os testes
./mvnw test

# Apenas Service ITs
./mvnw test -Dtest="*ServiceIT"

# Apenas Resource ITs
./mvnw test -Dtest="*ResourceIT"

# Teste específico
./mvnw test -Dtest="XpServiceIT#shouldApplyDailyCapWhenLimitReached"
```

### Requisitos para o PR

- Toda nova regra de negócio: ao menos um `*ServiceIT` cobrindo o caminho feliz e o(s) caso(s) de erro relevantes.
- Todo novo endpoint: ao menos um `*ResourceIT` cobrindo o caminho feliz (status 200/201) e um caso de erro (ex: 404, 409, 403).
- **Mocks de banco são proibidos** — os ITs usam H2 in-memory real com `@TestTransaction`.
- `*ServiceIT` usa `@TestTransaction` para rollback automático entre testes.
- `*ResourceIT` cria entidades com IDs únicos por teste (sem `@TestTransaction` — as chamadas HTTP fazem commit real).

---

## 8. Contribuição de Conteúdo

Todo conteúdo didático é distribuído sob a licença **CC BY-SA 4.0**.

### Como propor um curso

1. Consulte o quadro de "Cursos Necessários" na aba **Projects** do GitHub.
2. Abra uma Issue com a tag `content` descrevendo: tema, ementa por módulo e público-alvo.
3. Aguarde alinhamento pedagógico com os mantenedores.
4. Submeta o conteúdo via Pull Request (Markdown + links de vídeo) ou via painel do tutor na versão Beta.

### Padrões de qualidade

- **Áudio/vídeo:** som claro, sem ruídos de fundo, boa iluminação.
- **Didática:** exemplos práticos e exercícios ao final de cada módulo.
- **Linguagem:** inclusiva, respeitosa e acessível a iniciantes.

> Ao submeter via embedding do YouTube, você declara ser o autor do conteúdo ou possuir autorização para tal. O material de apoio (textos, exercícios, ementas) submetido ao repositório passa a seguir a CC BY-SA 4.0.

---

## 9. Reportar Vulnerabilidades

**Não abra Issues ou PRs públicos para vulnerabilidades de segurança.**

Envie um e-mail para **lucas.jdev1@gmail.com** com:
- Descrição da vulnerabilidade
- Módulo ou componente afetado
- Passos para reprodução (de forma responsável)
- Impacto potencial estimado

Responderemos em até 72 horas. Veja [SECURITY.md](SECURITY.md) para a política completa.

---

## Dúvidas?

- [Discord do Code Level]()
- [GitHub Discussions]()