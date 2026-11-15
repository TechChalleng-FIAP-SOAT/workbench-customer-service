# workbench-customer-service — Customer Service

Microsserviço de **clientes, veículos e identidade (usuários staff)** da plataforma
Workbench (Tech Challenge — FIAP Pós Tech, Fase 4). Dono do cadastro de clientes
(CPF/CNPJ) e veículos, com as validações de domínio, e também do **cadastro de
usuários staff** (ADMIN/TECHNICIAN) — a plataforma o usa como serviço de identidade.

> **Contexto (Fase 4):** na Fase 3 a oficina era um **monólito** (`workbench-monolith`,
> mantido como base histórica). Na Fase 4 a aplicação foi refatorada em
> **microsserviços independentes**, cada um com seu repositório, infraestrutura e
> banco próprios. Este repositório é o **Customer Service** — consultado por REST pelo
> OS Service e pela Lambda de autenticação. A divisão dos serviços e a estratégia da
> Saga estão nos ADRs (`workbench-monolith/docs/adr` — ADR-0006 e ADR-0005).

---

## Sumário

- [Responsabilidades](#responsabilidades)
- [Requisitos obrigatórios do PDF cobertos aqui](#requisitos-obrigatórios-do-pdf-cobertos-aqui)
- [Quem consome este serviço](#quem-consome-este-serviço)
- [Arquitetura da aplicação (hexagonal)](#arquitetura-da-aplicação-hexagonal)
- [Arquitetura de infraestrutura](#arquitetura-de-infraestrutura)
- [Fluxo de Deploy (CI/CD)](#fluxo-de-deploy-cicd)
- [Tecnologias](#tecnologias)
- [Como executar localmente](#como-executar-localmente)
- [Deploy em Kubernetes / AWS](#deploy-em-kubernetes--aws)
- [Referência de endpoints](#referência-de-endpoints)
- [Testes e cobertura](#testes-e-cobertura)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)

---

## Responsabilidades

- Cadastro e consulta de **clientes** (pessoa física/jurídica), com validação de
  **CPF/CNPJ** (dígitos verificadores) no value object de domínio
- Cadastro e consulta de **veículos**, com validação de **placa** (formato antigo e
  Mercosul) e **vínculo obrigatório** a um cliente existente (FK `vehicles.client_id`)
- Soft delete (desativação) de clientes e veículos
- **Identidade de staff**: cadastro de usuários ADMIN/TECHNICIAN com senha **BCrypt**
  e validação de credenciais (consumida pela Lambda de auth para emitir o JWT de
  staff). Preserva o login de staff que existia no monólito, agora como serviço de
  identidade dedicado. Senhas erradas seguidas bloqueiam o e-mail por um tempo, e a senha
  padrão do admin semeado é trocada no start pela de `ADMIN_INITIAL_PASSWORD`.

---

## Requisitos obrigatórios do PDF cobertos aqui

| Requisito (PDF Fase 4) | Como este serviço atende |
|---|---|
| ≥3 microsserviços independentes | É um dos 4 serviços, com repo/infra/banco próprios |
| Banco próprio | **PostgreSQL** exclusivo; nenhum outro serviço o acessa |
| SQL **e** NoSQL (no conjunto) | Contribui com o **SQL** (PostgreSQL) |
| Nenhum acesso ao banco de outro serviço | É o **dono** de cliente/veículo; os demais consultam por **REST** |
| REST + mensageria assíncrona | Expõe **REST**; é o lado síncrono da comunicação (consulta imediata) |
| Testes + 80% cobertura + Sonar | JaCoCo gate 80% no `verify` + SonarCloud (cobertura ~87%) |
| CI/CD + deploy K8s + branch protegida | Pipeline próprio; `main` protegida |

> Este serviço não participa da Saga por mensageria — ele é **consultado por REST**
> (ponto de validação síncrona), o que o PDF permite ("APIs RESTful quando necessário").

---

## Quem consome este serviço

| Consumidor | Chamada | Para quê |
|---|---|---|
| **OS Service** | `GET /api/clients/document/{cpf}` e `GET /api/vehicles/plate/{placa}` | Validar cliente/veículo ao criar a OS; resolver o e-mail do cliente para a notificação de status |
| **Lambda de auth (cliente)** | `GET /api/clients/document/{cpf}` | Resolver o cliente por CPF ao emitir o JWT de cliente |
| **Lambda de auth (staff)** | `POST /api/users/auth/validate` | Validar e-mail/senha do staff ao emitir o JWT de staff |

Todos deixaram de ler o banco diretamente (ADR-0006: nenhum serviço acessa o banco de
outro) e passaram a consultar este serviço por REST.

---

## Arquitetura da aplicação (hexagonal)

```
src/main/java/com/workbench/workshop/
├── adapter/
│   ├── in/web/            # ClientController, VehicleController, UserController + ClientDtos/VehicleDtos/UserDtos
│   └── out/persistence/   # Adapters JPA (client, vehicle, user)
├── application/           # Uma classe por caso de uso + ports (getById "ou 404" nas portas)
│   ├── Requester          # quem pede (subject do JWT + se é cliente) — regra de dono
│   ├── client/            # Create/FindById/FindByDocument/List/Update/DeactivateClient
│   ├── vehicle/           # Create/FindById/FindByPlate/ListByClient/Update/DeactivateVehicle
│   └── user/              # SignUp, AuthenticateUser, LoginAttemptLimiter
├── domain/                # Client + Document/Cpf/Cnpj; Vehicle + LicensePlate; User + Role; exceções
└── infrastructure/        # config (@UpperCase, GlobalExceptionHandler, PasswordConfig/BCrypt, AdminPasswordInitializer), persistence (JPA)
```

---

## Arquitetura de infraestrutura

Banco **PostgreSQL próprio** (FK `vehicles.client_id → clients.id`), namespace
`customer-service` no cluster EKS compartilhado `workbench-platform-<env>`. Sem acesso
cruzado a bancos.

---

## Fluxo de Deploy (CI/CD)

```
push (main/develop) ou PR
   │
   ▼
unit-test ──▶ code-analysis ──▶ build ──▶ docker-build-push ──▶ deploy-aws
(mvn verify   (JaCoCo gate 80%  (mvn      (imagem p/ Docker Hub  (EKS, só se
 + JaCoCo      + Sonar)          package)  usuario/customer-service) USE_AWS=true)
 badge)
```

Secrets/variáveis comuns aos serviços (`DOCKER_*`, `SONAR_TOKEN`, `AWS_*`, `USE_AWS`) —
ver o README do `workbench-os-service`. Próprios deste serviço: `DB_URL_CUSTOMER_<ENV>`,
`DB_USERNAME_CUSTOMER`, `DB_PASSWORD_CUSTOMER`, `JWT_SECRET` e
`ADMIN_INITIAL_PASSWORD` (necessário no 1º deploy; o serviço não sobe com o admin em `admin123`). O job `deploy-aws` monta o Secret do pod a partir deles (o
`k8s/secret.yaml` só tem placeholders) e falha no início se faltar um obrigatório.

---

## Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 + Spring Boot 4.0.9 | Framework web e DI |
| PostgreSQL 16 + Flyway | Banco próprio (SQL) + migrations |
| Micrometer + Datadog | Métricas e observabilidade |
| SpringDoc OpenAPI | Swagger UI |
| JaCoCo (gate 80%) + SonarCloud | Qualidade e cobertura |
| Testcontainers | Integração com PostgreSQL real |

---

## Como executar localmente

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

> **Sem valores padrão para segredos.** Fora do perfil `local` (`JWT_SECRET`, a senha do banco e `ADMIN_INITIAL_PASSWORD`) são
> obrigatórios: sem eles o serviço não sobe. O perfil `local` (`application-local.yml`)
> traz os valores de desenvolvimento e é ativado pelo compose do `workbench-messaging`.

O Flyway cria o schema (clientes, veículos e usuários) na subida.

| Variável | Default | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:15434/customer` | Banco próprio |
| `SPRING_DATASOURCE_USERNAME` | `dbadmin` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | — (obrigatório; dev no perfil `local`) | Senha do banco |
| `JWT_SECRET` | — (obrigatório; dev no perfil `local`) | Segredo HS256 compartilhado |
| `ADMIN_INITIAL_PASSWORD` | — (obrigatório fora do perfil `local`) | Substitui `admin123` do admin semeado no start; só enquanto ele ainda usa a padrão, nunca sobrescreve senha já trocada. Com o admin na senha padrão e esta variável vazia (ou com menos de 8 caracteres), o serviço **não sobe**; no perfil `local` só avisa |

---

## Deploy em Kubernetes / AWS

Manifests em `k8s/`. Deploy automatizado pelo pipeline (`deploy-aws`, quando
`USE_AWS=true`) no cluster `workbench-platform-<branch>`, namespace `customer-service`.

O que os manifests garantem:
- **Rede:** o Service é um **NLB interno** (`aws-load-balancer-internal`), alcançável só de dentro da VPC. O tráfego de fora entra pelo API Gateway (VPC Link + authorizer JWT); Swagger e actuator não ficam na internet. Se o Service já existia como LB público, o pipeline o recria uma vez (o esquema de um LB não muda depois de criado). Uma `NetworkPolicy` deixa entrar só a porta 8080 (vale com NetworkPolicy ligada no VPC CNI).
- **Imagem:** o pipeline troca o `REPLACE_IMAGE` do `app.yaml` pela tag do commit antes do `kubectl apply` (um rollout por deploy). Não há tag `:latest`; o `dd-java-agent` injetado pelo Datadog tem versão fixa (`v1.68.2`).
- **Pods:** usuário não-root (UID 10001), sem escalar privilégio, sem capabilities, sistema de arquivos somente leitura (só `/tmp` gravável), `seccomp` padrão. A subida tem um `startupProbe` (até 5 min); liveness e readiness começam depois. A JVM usa até 75% da memória do container (`-XX:MaxRAMPercentage=75.0`).
- **Escala:** o número de réplicas é só do HPA (o `Deployment` não fixa `replicas`), e um `PodDisruptionBudget` tira no máximo 1 pod por vez numa troca de nó.
- **Banco:** o RDS do `workbench-database` (URL e credenciais no Secret). O Postgres in-cluster saiu do `app.yaml` e ficou em `k8s/postgres-local.yaml`, só para testar num cluster sem RDS (o pipeline não aplica e remove o que sobrou de deploys antigos).

---

## Referência de endpoints

### Segurança (JWT)

Defesa em profundidade: o serviço **valida o JWT por conta própria** (Spring Security +
filtro HS256 com o `JWT_SECRET` compartilhado), inclusive nas chamadas internas de outros
serviços. Rotas **públicas** (sem token): `GET /actuator/health*`, Swagger/OpenAPI e
`POST /api/users/auth/validate` (a Lambda de auth valida credenciais aqui **antes** de
emitir o token). `POST /api/users/signup` exige **ADMIN**. A **listagem de todos os
clientes** (`GET /api/clients`) é só do staff. As demais consultas (`GET`) de
clientes/veículos aceitam cliente (`ROLE_CLIENT`) e staff, mas com token de cliente o
**dono é conferido no caso de uso**: só o próprio cadastro e os próprios veículos (o CPF do
token tem de ser o do cliente) — outro recebe **403**. Escrita exige ADMIN/TECHNICIAN.

**Limite de login:** 5 senhas erradas seguidas bloqueiam o e-mail por 15 minutos, com a
mesma resposta de credencial inválida (não revela o bloqueio). A contagem é por réplica.

> As chamadas servidor-a-servidor (ex.: o os-service consultando cliente/veículo) usam um
> **token de serviço** emitido pelo chamador com o mesmo segredo.

#### Boas práticas OWASP aplicadas
- **A01/A07:** autorização por role; ownership para cliente; JWT validado pelo serviço;
  limite de tentativas no login; senha padrão do admin substituída por variável de ambiente.
- **A02 (criptografia):** senha de staff com **BCrypt** (nunca em texto puro).
- **A03 (validação):** `@Valid` nos DTOs; CPF/CNPJ e placa validados (dígitos
  verificadores/regex); acesso a dados via Spring Data JPA parametrizado.
- **A05:** headers `X-Frame-Options: DENY` + `nosniff`; `k8s/secret.yaml` com
  placeholders (`REPLACE_ME`) — os valores reais vêm dos secrets da org, montados pela pipeline.
- **A09:** CPF mascarado no log (MDC).

> Hotspot `java:S4502` (CSRF disabled) marcado **Safe**: API REST stateless com JWT.

Clientes — `/api/clients`:

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/clients` | Criar cliente | staff |
| GET | `/api/clients` | Listar clientes ativos | staff |
| GET | `/api/clients/{id}` | Buscar por ID | cliente dono/staff |
| GET | `/api/clients/document/{document}` | Buscar por CPF ou CNPJ | cliente dono/staff |
| PUT | `/api/clients/{id}` | Atualizar nome/email/telefone | staff |
| DELETE | `/api/clients/{id}` | Desativar (soft delete) | staff |

Veículos — `/api/vehicles`:

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/vehicles` | Criar veículo | staff |
| GET | `/api/vehicles/{id}` | Buscar por ID | cliente dono/staff |
| GET | `/api/vehicles/plate/{plate}` | Buscar por placa | cliente dono/staff |
| GET | `/api/vehicles/client/{clientId}` | Listar veículos de um cliente | cliente dono/staff |
| PUT | `/api/vehicles/{id}` | Atualizar | staff |
| DELETE | `/api/vehicles/{id}` | Desativar (soft delete) | staff |

Identidade (staff) — `/api/users`:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/users/signup` | Cadastrar usuário staff (senha BCrypt; roles ADMIN/TECHNICIAN) — **só ADMIN** |
| POST | `/api/users/auth/validate` | Validar credenciais (uso interno da Lambda de auth) → `200 {email,roles}` ou `401` — **público** |

> **Seed inicial:** a migration cria o usuário `admin@workbench.dev` (role `ADMIN`,
> senha `admin123`) para viabilizar o primeiro login de staff. Fora do perfil `local`,
> `ADMIN_INITIAL_PASSWORD` é obrigatório: essa senha é trocada no primeiro start e o serviço
> não sobe enquanto o admin continuar com `admin123`.

---

## Testes e cobertura

```bash
./mvnw verify
```

- **Gate JaCoCo 80%** no `verify`.
- Integração com **Testcontainers PostgreSQL** (fluxo cliente→veículo + erros).
- Unitários cobrem a regra de dono (cliente só vê o próprio cadastro/veículo), o limite de
  login e a troca da senha do admin.
- Cobertura atual (medida em 2026-11-15): **87,0% de linhas** e 64,0% de branches (`target/site/jacoco/index.html`).
- Evidência por execução do CI: artefato `jacoco-report` do job `unit-test` (relatório HTML) e o Quality Gate do SonarCloud no job `code-analysis`.

---

## Documentação da API (Swagger)

| Recurso | URL (local) |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
