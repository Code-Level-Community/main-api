# ADR-023: Security Headers via ContainerResponseFilter

## Status
Accepted

## Context

APIs expostas à internet precisam de headers HTTP de segurança para mitigar ataques comuns: XSS via injeção de script, clickjacking via iframes, MIME sniffing, downgrade de HTTPS. Esses headers precisam ser aplicados globalmente a todas as respostas, sem exigir que cada endpoint os configure individualmente.

## Decision

Implementar um `@Provider ContainerResponseFilter` (`SecurityHeadersFilter`) que adiciona os seguintes headers a **toda resposta**:

| Header | Value (prod) | Purpose |
|---|---|---|
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' https://cdnjs.cloudflare.com; ...` | Bloqueia scripts e recursos de origens não autorizadas |
| `X-Content-Type-Options` | `nosniff` | Impede MIME type sniffing |
| `X-Frame-Options` | `DENY` | Bloqueia clickjacking via iframes |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS filter em browsers antigos |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Controla vazamento de URL no header Referer |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Força HTTPS (apenas em produção) |

### Comportamento por ambiente

O filtro detecta o ambiente via `@ConfigProperty(name = "app.environment")`:

- **Development**: CSP relaxado (`unsafe-inline`, `unsafe-eval`) para permitir HMR e Dev UI do Quarkus. HSTS **não** aplicado.
- **Production**: CSP restritivo. HSTS com `max-age=31536000`.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Configurar headers no reverse proxy (Nginx/Traefik) | Cria dependência de infraestrutura; headers não presentes em testes ou dev local |
| Configurar no `application.yml` via Quarkus HTTP | Suporte limitado — não permite lógica condicional por ambiente |
| Anotar cada endpoint individualmente | Impraticável; fácil de esquecer em novos endpoints |

## Motivation

Filter centralizado garante cobertura uniforme sem disciplina por endpoint. Diferenciação prod/dev evita bloquear o Dev UI do Quarkus em desenvolvimento.

## Consequences

### Positive
- Todos os endpoints automaticamente recebem os headers de segurança.
- Dev UI do Quarkus (`/q/dev/`) funciona sem conflitos com CSP relaxado em dev.
- Qualquer novo endpoint ou módulo é coberto sem configuração adicional.

### Negative / Trade-offs
- CSP hardcoded na classe — mudanças no domínio de produção exigem recompilação. Melhoria futura: externalizar via `@ConfigProperty`.
- `X-XSS-Protection` é considerado obsoleto em browsers modernos (substituído pelo CSP), mas mantido para compatibilidade com browsers antigos.
