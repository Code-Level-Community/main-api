# 🎓 Code Level Community

> #### Plataforma de cursos open-source onde a comunidade ensina e aprende junto.

Esta plataforma foi construída para democratizar o acesso ao ensino técnico e permitir que qualquer pessoa contribua, seja criando cursos (via embedding do YouTube) ou melhorando o código-fonte.

---

## 🌟 O Projeto
Nosso objetivo é criar um ecossistema onde o conteúdo didático e a tecnologia caminham juntos.

- **Educação Gratuita:** Cursos gerados pela comunidade.
- **Open-Source:** Código aberto sob licença **AGPLv3**.
- **Conteúdo Livre:** Material didático sob licença **CC BY-SA 4.0**.

---

## 🛠️ Tecnologias Utilizadas
O backend da plataforma utiliza o Quarkus (Supersonic Subatomic Java Framework), otimizado para GraalVM e deploys nativos.

- **Banco de Dados:** PostgreSQL (Produção) / H2 (Dev)
- **Migrações:** Flyway
- **ORM:** Hibernate com Panache
- **Configuração:** YAML

---

## 🚀 Como rodar o projeto localmente
### Pré-requisitos
- **Java 17+** instalado
- **Docker** (opcional, para o banco de dados)

### Modo de Desenvolvimento

```shell
./mvnw quarkus:dev
```

Acesse a interface de dev em: http://localhost:8080/q/dev/

### Gerando o Executável Nativo (GraalVM)
Para máxima performance e baixo consumo de memória:

```shell
./mvnw package -Dnative
```
## 🤝 Como contribuir
Adoramos contribuições! Você pode ajudar de duas formas:

1. **Desenvolvimento:** Corrija bugs ou implemente novas features. Consulte o nosso [Manual de Contribuição](CONTRIBUITING.md).
2. **Conteúdo:** Tem algo para ensinar? Sugira um curso através das nossas [Issues]().

**Antes de começar, leia nosso [Código de Conduta](CODE_OF_CONDUCT.md).**

---

## 📜 Licenciamento
Este projeto é distribuído sob as seguintes licenças:

- Software: [GNU AGPLv3](LICENSE)
- Conteúdo Didático: CC BY-SA 4.0

---

## 💬 Contato & Comunidade
- Discord: [Code level]()
- GitHub Discussions: [discussões]()
