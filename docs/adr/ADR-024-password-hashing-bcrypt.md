# ADR-024: Hash de Senha com BCrypt (custo 12)

## Status
Accepted

## Context

Senhas dos usuários precisam ser armazenadas de forma que: mesmo com acesso ao banco, um atacante não consiga recuperar as senhas originais; ataques de força bruta e rainbow tables sejam inviáveis; e o custo computacional seja ajustável conforme o hardware evolui.

## Decision

Usar **BCrypt com fator de custo 12** implementado via `io.quarkus.elytron.security.common.BcryptUtil`.

Encapsulado em `QuarkusBcryptPasswordHasher` que implementa a interface de domínio `PasswordHasher`:

```java
public class QuarkusBcryptPasswordHasher implements PasswordHasher {
    private static final int COST = 12;

    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword, COST);
    }

    public boolean verify(String rawPassword, String hashedPassword) {
        return BcryptUtil.matches(rawPassword, hashedPassword);
    }
}
```

A interface `PasswordHasher` é injetada no domínio — o domínio nunca depende da implementação BCrypt diretamente.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| MD5 / SHA-1 / SHA-256 | Funções hash criptográficas rápidas — permitem ataques de força bruta com GPUs a bilhões de tentativas/segundo |
| Argon2id | Algoritmo mais moderno e recomendado pelo OWASP, mas sem suporte nativo no Quarkus/Elytron |
| SCrypt | Mesma limitação que Argon2id; complexidade de configuração maior |
| BCrypt custo 10 | Custo padrão de muitos frameworks; custo 12 oferece 4× mais resistência sem impacto perceptível no login |
| PBKDF2 | Suportado em Java nativo, mas inferior ao BCrypt contra ataques em GPUs/ASICs |

## Motivation

- **BCrypt é memory-hard por design**: limita paralelismo em GPU/ASIC, tornando ataques de força bruta caros mesmo com hardware dedicado.
- **Fator 12** resulta em ~200-400ms de hash em hardware moderno — imperceptível para usuários, mas caro para atacantes iterando milhões de senhas.
- **Suporte nativo no Quarkus** via `quarkus-elytron-security-common` — sem dependências extras.
- **Interface `PasswordHasher`** desacopla o domínio da implementação — possível migrar para Argon2id no futuro sem alterar lógica de negócio.

## Consequences

### Positive
- BCrypt custo 12 exige anos de computação em GPU dedicada para quebrar hashes por força bruta.
- A interface `PasswordHasher` permite migrar para Argon2id no futuro sem alterar lógica de negócio.
- Senhas existentes não precisam ser invalidadas ao mudar o custo; BCrypt armazena o custo no próprio hash.

### Negative / Trade-offs
- Login tem custo fixo de ~200-400ms para hash comparison — aceitável para autenticação interativa.
- Não há migração automática de hashes legados ao aumentar o custo; usuários terão seus hashes atualizados apenas no próximo login bem-sucedido (re-hash on login).
