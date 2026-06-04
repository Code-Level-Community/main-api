# Política de Segurança

## Versões com Suporte Ativo

| Branch | Suporte |
|---|---|
| `main` | Ativo — correções aplicadas aqui |

## Reportando uma Vulnerabilidade

**Não abra Issues ou Pull Requests públicos para vulnerabilidades de segurança.** Uma divulgação pública antes da correção coloca todos os usuários em risco.

### Como reportar

Envie um e-mail para **lucas.jdev1@gmail.com** com o assunto:

```
[SECURITY] <título curto da vulnerabilidade>
```

Inclua na mensagem:
- **Descrição** da vulnerabilidade e qual dado ou componente é afetado
- **Módulo** do sistema afetado (ex: `identity`, `gamification`, etc.)
- **Passos para reprodução** de forma responsável (não explore além do necessário para demonstrar)
- **Impacto potencial** estimado (ex: exposição de dados, bypass de autenticação)
- **Versão / commit** afetado, se conhecido

### O que esperar

| Prazo | Ação |
|---|---|
| 72 horas | Confirmação de recebimento e triagem inicial |
| 7 dias | Avaliação de severidade e plano de resposta |
| 30 dias | Correção publicada para severidade crítica ou alta |
| 90 dias | Correção publicada para severidade média ou baixa |

### Reconhecimento

Ao reportar de forma responsável — sem divulgação pública antes da publicação da correção — seu nome será creditado no changelog da correção, caso deseje.

### Fora do escopo

Os seguintes itens **não são** considerados vulnerabilidades neste projeto:

- Ataques que requerem acesso físico ao servidor
- Vulnerabilidades em dependências de terceiros já com CVE público (abra uma issue normal)
- Ataques de negação de serviço que exigem largura de banda massiva
- Problemas de segurança no ambiente de desenvolvimento local (H2 in-memory, Dev UI)