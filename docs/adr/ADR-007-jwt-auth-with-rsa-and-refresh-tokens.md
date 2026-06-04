# ADR-007: Autenticação JWT com RSA e Refresh Tokens

## Status
Accepted

## Context

O sistema precisa de autenticação segura e stateless. Tokens de acesso de longa duração criam janela grande para abuso se roubados. Tokens de curta duração sem mecanismo de renovação forçam re-login frequente, degradando UX.

## Decision

Implementar autenticação stateless com **dois tipos de token**:

### Access Token (JWT + RSA)
- Assinado com chave privada RSA (`META-INF/resources/privateKey.pem`).
- Verificado com chave pública RSA (`META-INF/resources/publicKey.pem`).
- TTL: **15 minutos**.
- Biblioteca: SmallRye JWT (MicroProfile JWT spec).
- Claim `sub` carrega o UUID público do usuário.
- Validado automaticamente pelo Quarkus via `@RolesAllowed` e `@PermitAll`.

### Refresh Token
- UUID aleatório gerado no login.
- Persistido na tabela `CL_REFRESH_TOKEN` com data de expiração.
- TTL: **7 dias**.
- Revogado no logout (deletado do banco).
- Endpoint `/api/auth/refresh` aceita o refresh token e emite novo par de tokens.

```
POST /api/auth/login     → { accessToken, refreshToken }
POST /api/auth/refresh   → { accessToken, refreshToken }  (invalida o anterior)
POST /api/auth/logout    → revoga o refreshToken no banco
```

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Sessões no servidor | Requer armazenamento de estado por usuário — incompatível com escalonamento horizontal e native image |
| JWT simétrico (HMAC) | Qualquer serviço com a chave pode assinar tokens — risco em sistemas com múltiplos componentes |
| Access token de longa duração (sem refresh) | Janela longa de abuso se o token for roubado; re-login ao expirar |

## Motivation

RSA assimétrico permite que componentes de leitura validem tokens sem acesso à chave privada. O refresh token revogável viabiliza logout real mesmo com JWT stateless.

## Consequences

### Positive
- Stateless: servidores não armazenam sessões; escalável horizontalmente.
- RSA assimétrico: componentes de leitura validam tokens sem precisar da chave privada.
- Refresh token revogável: logout real é possível mesmo com JWT stateless.
- Compatível com GraalVM native (sem reflexão especial necessária).
- MicroProfile JWT integra com `@RolesAllowed` nativamente no Quarkus.

### Negative / Trade-offs
- Access tokens não são revogáveis antes do TTL de 15 min (janela de abuso para tokens roubados).
- Par de chaves RSA precisa ser gerenciado com segurança em produção (rotação, segredos).
- As chaves em `META-INF/resources/` não devem ser commitadas para repositórios públicos.
- Refresh token exige uma query no banco a cada renovação (ponto de estado no sistema stateless).
- Rotação de refresh token (emitir novo no uso) invalida tokens antigos — clientes com múltiplas abas podem ter problemas.