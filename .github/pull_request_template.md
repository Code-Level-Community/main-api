## Descrição

<!-- Descreva as mudanças neste PR em 2-3 frases. O QUÊ mudou e o PORQUÊ. -->

Closes #<!-- número da issue -->

---

## Tipo de mudança

- [ ] Correção de bug (`fix/`)
- [ ] Nova funcionalidade (`feature/`)
- [ ] Refatoração / melhoria (`improvement/`)
- [ ] Documentação (`docs/`)
- [ ] Outro: ___

---

## Como testar

<!-- Descreva os passos para um revisor verificar que as mudanças funcionam.
     Ex: "Rode POST /api/gamification/xp/award com o body abaixo e verifique que retorna 201 com leveledUp: true" -->

---

## Checklist

### Código
- [ ] Todos os testes passam localmente (`./mvnw test`)
- [ ] Nova lógica de negócio tem `*ServiceIT`
- [ ] Novo endpoint tem `*ResourceIT` (caminho feliz + ao menos 1 caso de erro)
- [ ] Sem lógica de negócio nos controllers (`*Resource`)
- [ ] Módulos se comunicam apenas via REST (sem `@Inject` cross-module)
- [ ] Query methods retornam `Optional<T>` (nunca `null`)
- [ ] Domain objects validam no construtor / factory method
- [ ] Exceptions de domínio (`BusinessRuleException`, `ResourceNotFound`, etc.) — não `WebApplicationException`
- [ ] Constructor injection utilizado (sem `@Inject` em campo)

### Banco de dados
- [ ] Mudanças de schema têm migration Flyway em `db/migration/V{n}__*.sql`
- [ ] Sem breaking changes em colunas/tabelas existentes sem migration correspondente

### Documentação
- [ ] ADR criado/atualizado se esta mudança envolve uma decisão arquitetural relevante
- [ ] `CONTRIBUTING.md` atualizado se o fluxo de contribuição mudou