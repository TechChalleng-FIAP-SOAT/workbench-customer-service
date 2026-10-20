# workbench-customer-service

Customer Service da plataforma Workbench (Tech Challenge Fase 4). Dono dos
**clientes** (CPF/CNPJ) e dos **veículos**, extraídos dos módulos `client` e
`vehicle` do monólito. Banco próprio (**PostgreSQL**), arquitetura hexagonal,
pacote `com.workbench.workshop`.

## Responsabilidade

- Cadastro e consulta de **clientes** (pessoa física/jurídica), com validação de
  CPF/CNPJ (dígitos verificadores) no value object de domínio.
- Cadastro e consulta de **veículos**, com validação de placa (formato antigo e
  Mercosul) e vínculo obrigatório a um cliente existente (FK `vehicles.client_id`).
- É **consultado por REST** pelo OS Service para validar cliente/veículo na
  criação da OS (não participa da Saga por mensageria).

## API

Clientes — `/api/clients`:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/clients` | Cria cliente (201) |
| GET | `/api/clients` | Lista clientes ativos |
| GET | `/api/clients/{id}` | Busca por ID |
| GET | `/api/clients/document/{document}` | Busca por CPF ou CNPJ (aceita formatado) |
| PUT | `/api/clients/{id}` | Atualiza nome/email/telefone |
| DELETE | `/api/clients/{id}` | Desativa (soft delete, 204) |

Veículos — `/api/vehicles`:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/vehicles` | Cria veículo (201) |
| GET | `/api/vehicles/{id}` | Busca por ID |
| GET | `/api/vehicles/plate/{plate}` | Busca por placa |
| GET | `/api/vehicles/client/{clientId}` | Lista veículos ativos de um cliente |
| PUT | `/api/vehicles/{id}` | Atualiza |
| DELETE | `/api/vehicles/{id}` | Desativa (soft delete, 204) |

Swagger UI em `/swagger-ui.html`.

## Rodar local

Pré-requisito: um PostgreSQL acessível (ver `application.yml`, default
`jdbc:postgresql://localhost:15434/customer`). Flyway cria o schema (migration V1).

```bash
./mvnw spring-boot:run
```

## Testes

```bash
./mvnw verify
```

Unitários (domínio/use cases) + integração com **Testcontainers (PostgreSQL)**.
Gate de cobertura JaCoCo em 80% (relatório gerado na fase `verify`).

## Deploy

- `Dockerfile` multi-stage (build Maven + runtime Corretto 21).
- Manifests em `k8s/` (namespace, configmap, secret, deployment+service, HPA).
- Pipeline em `.github/workflows/pipeline.yml` (test → sonar/gate → build →
  imagem Docker → deploy EKS quando `USE_AWS=true`).

> Provisionar na AWS e configurar secrets do pipeline são ações do usuário — ver
> `PENDENCIAS.md` na raiz do workspace.
