# ADR-021: Módulo de Certificate

## Status
Accepted

## Context

O módulo `certificate` gerencia a emissão de certificados de conclusão de curso. A geração de um certificado (potencialmente um PDF com assinatura digital) é uma operação que pode levar segundos ou mais, tornando o processamento assíncrono uma necessidade, não uma otimização.

---

## Decision 1: Processamento assíncrono via Vert.x Event Bus com commit antes do dispatch

**What:** O `requestCertificate()` divide o processo em duas etapas: (1) `persistPending()` — anotado com `@Transactional` — persiste a entidade com status `PENDING` e faz commit; (2) após o commit, `eventBus.send("certificate.process", certificateId)` dispara o evento para o processador assíncrono. O `202 ACCEPTED` é retornado imediatamente ao cliente.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Processamento síncrono dentro do request HTTP | Bloqueia o thread por segundos, prejudicando throughput e podendo causar timeout no cliente |
| Dispatch do evento antes do commit | O processador assíncrono poderia tentar buscar o certificado antes do commit tornar o registro visível |
| Job scheduler periódico (polling em PENDING) | Introduz latência até a próxima execução do job e dependência de infraestrutura de scheduler |

**Motivation:** O commit antes do dispatch garante que o processador assíncrono sempre encontra o registro ao buscar pelo ID. O Event Bus do Vert.x é in-process, portanto o dispatch é confiável sem broker externo.

**Consequences:**
- (+) `202 ACCEPTED` imediato para o cliente sem bloquear thread HTTP
- (+) Sem race condition entre persistência e processamento assíncrono
- (+) Sem infraestrutura externa: Event Bus Vert.x disponível nativamente no Quarkus
- (-) Se a JVM reiniciar entre o commit e o dispatch, o certificado fica preso em `PENDING` sem reprocessamento automático
- (-) Falha no processador assíncrono pode deixar certificados em `PROCESSING` indefinidamente sem notificação ao usuário

---

## Decision 2: UUID como código de verificação público

**What:** `CertificateEntity` armazena um `certificate_code` (UUID) com constraint `UNIQUE`. O endpoint público `GET /certificates/verify/{code}` usa esse código como chave de acesso, sem exigir autenticação.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| ID sequencial como código (`CERT-000123`) | Previsível e enumerável; qualquer pessoa poderia percorrer todos os certificados |
| Hash do conteúdo (UUID v5 baseado em user_id + course_id) | Determinístico; requer que o verificador conheça user_id e course_id para calcular o hash |
| JWT assinado como certificado | Auto-contido, mas mais complexo de revogar e difícil de exibir em QR code |

**Motivation:** UUIDs aleatórios são impraticáveis de adivinhar (2^122 possibilidades). O endpoint de verificação pública sem autenticação permite que empregadores verifiquem certificados sem conta na plataforma.

**Consequences:**
- (+) Verificação pública sem autenticação possível via URL com o UUID
- (+) Proteção contra enumeração
- (+) URL amigável para compartilhar (ex: QR code em LinkedIn)
- (-) Se o `certificate_code` vazar, qualquer pessoa pode ver os dados do certificado; informações exibidas devem ser limitadas ao necessário

---

## Decision 3: Snapshot de `user_email` e `user_name` no momento da solicitação

**What:** Na criação do certificado, `user_email` e `user_name` são copiados do JWT atual e armazenados na entidade. Esses valores são imutáveis após a persistência e não referenciados via join com `CL_USER`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Resolver nome/email via join com `CL_USER` em tempo de geração | Cria dependência cross-module; o certificado gerado poderia conter informações diferentes das do momento da conquista |
| Armazenar apenas `user_id` e resolver via REST client | O processador assíncrono precisaria fazer chamada HTTP para obter os dados na hora de gerar o PDF |

**Motivation:** Um certificado é um documento histórico. O nome no certificado deve ser o nome do usuário no momento da conquista. O snapshot garante imutabilidade e elimina dependência runtime do módulo `identity` no processador assíncrono.

**Consequences:**
- (+) Certificado gerado com dados do momento da conquista, mesmo que o usuário mude o nome depois
- (+) Processador assíncrono não precisa fazer chamadas HTTP para buscar dados do usuário
- (-) Se o usuário quiser corrigir o nome no certificado após emissão, requer lógica adicional de re-emissão
- (-) `user_email` pode ficar desatualizado se o usuário mudar o email; sem sincronização automática

---

## Decision 4: Verificação de conclusão de curso via REST client antes de solicitar certificado

**What:** O `CertificateResource.requestCertificate()` chama `EnrollmentClient.getByUserAndCourse()` no módulo `student_progress` para verificar que o aluno completou o curso (`completedAt IS NOT NULL`) antes de chamar `certificateService.requestCertificate()`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Verificar dentro do `CertificateService` | Serviços não fazem chamadas cross-module; a verificação pertence à camada de entrada (resource) |
| Confiar no cliente (não validar) | Permite emissão de certificado sem ter completado o curso |
| Evento de conclusão dispara automaticamente a solicitação | Desacopla totalmente, mas requer infraestrutura de eventos e o aluno perde controle sobre quando emitir |

**Motivation:** A validação no resource é consistente com a arquitetura onde serviços são agnósticos a outros módulos. O `CertificateService` permanece autossuficiente.

**Consequences:**
- (+) Certificados só são emitidos para alunos que completaram o curso
- (+) O serviço `CertificateService` permanece agnóstico ao módulo `student_progress`
- (-) O resource contém lógica de validação de negócio além de I/O HTTP; exceção justificada à regra de recursos finos
- (-) Se o módulo `student_progress` estiver indisponível, certificados não podem ser solicitados

---

## Decision 5: Retry via re-solicitação de certificados com status FAILED

**What:** Certificados com status `FAILED` podem ser re-solicitados. A lógica em `persistPending()` verifica se já existe um certificado com status `SENT` ou `PROCESSING`. Se existir um `FAILED`, ele é substituído por uma nova entrada `PENDING`.

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|
| Update do registro FAILED para PENDING (sem criar novo) | Reutiliza o mesmo ID, mas perde o histórico da falha anterior (error_message) |
| Proibir retry (certificado FAILED é definitivo) | Falhas de infraestrutura não devem bloquear a obtenção de um certificado ganho |
| Retry automático pelo processador | Mais transparente, mas requer lógica de backoff e limite de tentativas |

**Motivation:** Falhas de geração são geralmente transitórias. Permitir retry explícito pelo usuário é a solução de menor complexidade que resolve o problema sem automatismos que possam gerar loops infinitos.

**Consequences:**
- (+) Usuário pode obter seu certificado mesmo após falha transitória
- (+) Histórico da falha (error_message) preservado no registro FAILED anterior para diagnóstico
- (-) Um mesmo usuário pode ter múltiplos registros FAILED para o mesmo curso no banco; sem limite de registros históricos
- (-) Sem notificação proativa ao usuário quando o certificado falha; ele precisa verificar o status manualmente
