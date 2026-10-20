# workbench-customer-service — Customer Service

Microsserviço de **clientes e veículos** da plataforma Workbench (Tech Challenge —
FIAP Pós Tech, Fase 4). Dono do cadastro de clientes (CPF/CNPJ) e veículos, com as
validações de domínio.

> **Contexto (Fase 4):** na Fase 3 a oficina era um **monólito** (`workbench-monolith`,
> mantido como base histórica). Na Fase 4 a aplicação foi refatorada em
> **microsserviços independentes**, cada um com seu repositório, infraestrutura e
> banco próprios. Este repositório é o **Customer Service** — consultado por REST pelo
> OS Service e pela Lambda de autenticação. A divisão dos serviços e a estratégia da
> Saga estão nos ADRs (`workbench-monolith/docs/adr` — ADR-0003 e ADR-0005).

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
| **OS Service** | `GET /api/clients/document/{cpf}` e `GET /api/vehicles/plate/{placa}` | Validar cliente/veículo ao criar a OS |
| **Lambda de auth** | `GET /api/clients/document/{cpf}` | Resolver o cliente por CPF ao emitir o JWT |

Ambos deixaram de ler a tabela de clientes diretamente (ADR-0003: nenhum serviço
acessa o banco de outro) e passaram a consultar este serviço por REST.

---

## Arquitetura da aplicação (hexagonal)

```
src/main/java/com/workbench/workshop/
├── adapter/
│   ├── in/web/            # ClientController, VehicleController (+ DTOs)
│   └── out/persistence/   # Adapters JPA (client, vehicle)
├── application/           # Use cases + ports (ClientUseCases, VehicleUseCases)
├── domain/                # Client + Document/Cpf/Cnpj; Vehicle + LicensePlate; exceções
└── infrastructure/        # config (@UpperCase, GlobalExceptionHandler), persistence (JPA)
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

Secrets/variáveis idênticos aos demais serviços (`DOCKER_*`, `SONAR_TOKEN`, `AWS_*`,
`USE_AWS`).

---

## Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 + Spring Boot 4.0.5 | Framework web e DI |
| PostgreSQL 16 + Flyway | Banco próprio (SQL) + migrations |
| Micrometer + Datadog | Métricas e observabilidade |
| SpringDoc OpenAPI | Swagger UI |
| JaCoCo (gate 80%) + SonarCloud | Qualidade e cobertura |
| Testcontainers | Integração com PostgreSQL real |

---

## Como executar localmente

```bash
./mvnw spring-boot:run
```

O Flyway cria o schema (clientes + veículos) na subida. Datasource configurável por
`SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:15434/customer`).

---

## Deploy em Kubernetes / AWS

Manifests em `k8s/`. Deploy automatizado pelo pipeline (`deploy-aws`, quando
`USE_AWS=true`) no cluster `workbench-platform-<branch>`, namespace `customer-service`.

---

## Referência de endpoints

Clientes — `/api/clients`:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/clients` | Criar cliente |
| GET | `/api/clients` | Listar clientes ativos |
| GET | `/api/clients/{id}` | Buscar por ID |
| GET | `/api/clients/document/{document}` | Buscar por CPF ou CNPJ |
| PUT | `/api/clients/{id}` | Atualizar nome/email/telefone |
| DELETE | `/api/clients/{id}` | Desativar (soft delete) |

Veículos — `/api/vehicles`:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/vehicles` | Criar veículo |
| GET | `/api/vehicles/{id}` | Buscar por ID |
| GET | `/api/vehicles/plate/{plate}` | Buscar por placa |
| GET | `/api/vehicles/client/{clientId}` | Listar veículos de um cliente |
| PUT | `/api/vehicles/{id}` | Atualizar |
| DELETE | `/api/vehicles/{id}` | Desativar (soft delete) |

---

## Testes e cobertura

```bash
./mvnw verify
```

- **Gate JaCoCo 80%** no `verify`.
- Integração com **Testcontainers PostgreSQL** (fluxo cliente→veículo + erros).
- Cobertura atual: **~87%** (`target/site/jacoco/index.html`).

---

## Documentação da API (Swagger)

| Recurso | URL (local) |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
