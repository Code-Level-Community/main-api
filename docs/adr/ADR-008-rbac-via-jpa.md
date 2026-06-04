# ADR-008: Controle de Acesso Baseado em Papéis (RBAC) via JPA

## Status
Accepted

## Context

O sistema precisa de autorização granular: diferentes usuários têm diferentes permissões de acesso a recursos. O Quarkus Security JPA integra nativamente com entidades JPA para resolver papéis do usuário autenticado.

## Decision

Implementar RBAC com **entidades JPA** gerenciadas pelo banco de dados:

### Modelo de dados
```
UserEntity  ──ManyToMany──  RoleEntity  ──ManyToMany──  PermissionEntity
(CL_USER)                  (CL_ROLE)                   (CL_PERMISSION)
```

```java
@Entity @Table(name = "CL_ROLE")
public class RoleEntity extends PanacheEntityBase {
    public String name;  // ex: "ADMIN", "USER", "INSTRUCTOR"

    @ManyToMany(fetch = FetchType.EAGER)
    public List<PermissionEntity> permissions;
}
```

### Enforcement na camada REST
```java
@GET
@Path("/admin/users")
@RolesAllowed("ADMIN")
public List<UserResponse> listAll() { ... }

@POST
@Path("/courses")
@RolesAllowed({"ADMIN", "INSTRUCTOR"})
public Response createCourse(...) { ... }
```

### Bootstrap de papéis padrão
`UserEntityInitializer` (listener de `StartupEvent`) cria roles e permissões padrão na inicialização: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_INSTRUCTOR`.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Papéis hardcoded (enum) | Não configurável em runtime; requer redeploy para adicionar ou modificar papéis |
| ABAC (Attribute-Based Access Control) | Mais expressivo, mas complexidade excessiva para o estágio atual do projeto |

## Motivation

Roles e permissões configuráveis em runtime via banco, com integração nativa ao `@RolesAllowed` do Quarkus Security JPA, sem código de autorização espalhado pelos serviços.

## Consequences

### Positive
- Roles e permissões configuráveis em runtime via banco — sem necessidade de redeploy.
- Integração nativa com `@RolesAllowed` do Quarkus Security JPA.
- Granularidade de permissão além do papel: `PermissionEntity` permite controle fino.
- Auditoria de mudanças de acesso via histórico do banco de dados.
- Bootstrap automático garante roles mínimas presentes em qualquer ambiente.

### Negative / Trade-offs
- `FetchType.EAGER` em roles carrega permissões em toda query de usuário — pode ser custoso com muitas permissões.
- Cache de segurança não é automático: se roles mudam no banco, o usuário precisa fazer novo login para o JWT refletir os papéis atualizados.
- `@RolesAllowed` verifica os claims do JWT — não a role atual no banco — então mudanças de role só valem no próximo token.
- Lógica de autorização mais complexa (ex: "só o dono do recurso pode editar") não é coberta por `@RolesAllowed` e precisa de verificação manual no serviço.
