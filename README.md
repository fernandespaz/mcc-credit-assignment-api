#MCC Credit Assignment API

> **API RESTful para Cessão de Crédito** — Motor de precificação com Strategy Pattern, liquidações ACID-safe com lock pessimista, motor de câmbio e relatórios analíticos com SQL nativo. Arquitetura hexagonal simplificada em três camadas.

---

## Índice

- [Visão Geral](#visão-geral)
- [Stack Tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
  - [Diagrama de Camadas](#diagrama-de-camadas)
  - [Estrutura de Pacotes](#estrutura-de-pacotes)
  - [Fluxo de uma Requisição](#fluxo-de-uma-requisição)
- [Domínio do Negócio](#domínio-do-negócio)
  - [Entidades](#entidades)
  - [Motor de Precificação (Strategy Pattern)](#motor-de-precificação-strategy-pattern)
  - [Motor de Câmbio](#motor-de-câmbio)
  - [Liquidação ACID](#liquidação-acid)
- [API REST](#api-rest)
  - [Autenticação e Autorização](#autenticação-e-autorização)
  - [Endpoints](#endpoints)
  - [Documentação Swagger](#documentação-swagger)
- [Relatórios Analíticos (Camada de 2 Níveis)](#relatórios-analíticos-camada-de-2-níveis)
- [Banco de Dados](#banco-de-dados)
  - [Perfil de Desenvolvimento (H2)](#perfil-de-desenvolvimento-h2)
  - [Perfil de Produção (AWS RDS)](#perfil-de-produção-aws-rds)
- [Como Executar](#como-executar)
  - [Pré-requisitos](#pré-requisitos)
  - [Executando Localmente](#executando-localmente)
  - [Executando em Produção (JAR direto)](#executando-em-produção-jar-direto)
  - [Executando em Docker com AWS RDS](#executando-em-docker-com-aws-rds)
- [Exemplos de Uso](#exemplos-de-uso)
- [Decisões Técnicas](#decisões-técnicas)
- [Tratamento de Erros](#tratamento-de-erros)
- [ADR — Architecture Decision Records](#adr--architecture-decision-records)
  - [ADR-001: SQL Relacional vs NoSQL](#adr-001-sql-relacional-vs-nosql)
  - [ADR-002: Monolito Modular vs Microserviços](#adr-002-monolito-modular-vs-microserviços)
  - [ADR-003: Arquitetura Hexagonal vs MVC Tradicional](#adr-003-arquitetura-hexagonal-vs-mvc-tradicional)
  - [ADR-004: Lock Pessimista vs Otimista para Liquidações](#adr-004-lock-pessimista-vs-otimista-para-liquidações)
  - [ADR-005: Strategy Pattern vs Configuração por Banco de Dados](#adr-005-strategy-pattern-vs-configuração-por-banco-de-dados)
- [Design de Alta Escala — 1 Milhão de Transações/Minuto](#design-de-alta-escala--1-milhão-de-transaçõesminuto)
  - [Contexto e Desafios](#contexto-e-desafios)
  - [Arquitetura Alvo](#arquitetura-alvo)
  - [Caching](#caching)
  - [Sharding e Particionamento](#sharding-e-particionamento)
  - [Consistência Eventual](#consistência-eventual)
  - [Estimativa de Capacidade](#estimativa-de-capacidade)
- [Modelagem de Eventos — EDA](#modelagem-de-eventos--eda)
  - [Por que Event-Driven?](#por-que-event-driven)
  - [Eventos de Domínio](#eventos-de-domínio)
  - [Diagrama de Fluxo de Eventos](#diagrama-de-fluxo-de-eventos)
  - [Padrões Aplicados](#padrões-aplicados)
- [IaC — Kubernetes Manifests](#iac--kubernetes-manifests)

---

## Visão Geral

Esta API implementa o backend de um sistema de **cessão de crédito (factoring)**, onde uma empresa cede seus recebíveis (duplicatas, cheques pré-datados) a uma instituição financeira em troca de liquidez antecipada. O sistema calcula o **valor presente** do título descontando juros compostos com um spread de risco que varia conforme o tipo do recebível.

### Funcionalidades Principais

| Funcionalidade | Descrição |
|---|---|
| 🏦 **Motor de Câmbio** | Cadastro e sincronização de taxas de câmbio (manual e mock de API externa) |
| 📐 **Motor de Precificação** | Cálculo de valor presente com spreads por tipo de recebível (Strategy Pattern) |
| 🔒 **Liquidações ACID** | Liquidação transacional com lock pessimista, prevenindo race conditions |
| 📊 **Relatórios Analíticos** | Extrato de liquidações com filtros e SQL nativo otimizado |
| 📄 **OpenAPI / Swagger** | Documentação interativa completa da API |

---

## Stack Tecnológica

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2 |
| Persistência | Spring Data JPA + Hibernate 6 |
| Banco (dev) | H2 (in-memory) |
| Banco (prod) | Amazon RDS |
| Documentação | springdoc-openapi 2.5 (Swagger UI) |
| Build | Maven 3.9+ |
| Utilitários | Lombok |

---

## Arquitetura

O projeto adota uma **arquitetura hexagonal simplificada** (também chamada de Ports & Adapters), organizada em três camadas com responsabilidades bem definidas.

### Diagrama de Camadas

```
┌─────────────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE                             │
│                                                                 │
│  ┌─────────────────────┐      ┌───────────────────────────────┐ │
│  │   REST Controllers   │      │   JPA Entities & Repositories │ │
│  │  (Primary Adapters)  │      │   (Secondary Adapters)        │ │
│  └──────────┬──────────┘      └───────────────┬───────────────┘ │
└─────────────│──────────────────────────────────│─────────────────┘
              │ implements port/in                │ implements port/out
┌─────────────▼──────────────────────────────────▼─────────────────┐
│                       APPLICATION                                 │
│                                                                   │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  Use Cases (AssignorApplicationService,                  │   │
│   │             SettlementApplicationService, ...)           │   │
│   │  DTOs (Records de request/response)                      │   │
│   └──────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
              │ calls domain services & entities
┌─────────────▼─────────────────────────────────────────────────────┐
│                         DOMAIN                                     │
│                                                                    │
│  ┌──────────────┐  ┌───────────────┐  ┌────────────────────────┐  │
│  │   Entities   │  │     Ports     │  │   Pricing Strategies   │  │
│  │  (POJOs sem  │  │  (in / out)   │  │  (Strategy Pattern)    │  │
│  │  framework)  │  │  interfaces   │  │                        │  │
│  └──────────────┘  └───────────────┘  └────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

> **Regra de dependência:** as setas apontam sempre para dentro. O Domínio não conhece Spring, JPA ou qualquer framework. A Infraestrutura depende das interfaces (portas) definidas no Domínio.

### Estrutura de Pacotes

```
src/main/java/com/srm/mcc/credit/
│
├── CreditAssignmentApplication.java        # Entrypoint Spring Boot
│
├── domain/                                 # ← Núcleo de negócio puro (zero Spring)
│   ├── entity/
│   │   ├── Assignor.java                   # Cedente do crédito
│   │   ├── Receivable.java                 # Título (duplicata, cheque)
│   │   ├── Settlement.java                 # Registro de liquidação
│   │   └── ExchangeRate.java               # Taxa de câmbio
│   ├── enums/
│   │   ├── ReceivableType.java             # DUPLICATA | POST_DATED_CHECK
│   │   ├── Currency.java                   # BRL | USD | EUR
│   │   └── SettlementStatus.java           # PENDING | SETTLED | CANCELLED
│   ├── exception/
│   │   ├── DomainException.java            # Base abstrata
│   │   ├── AssignorNotFoundException.java
│   │   ├── ReceivableNotFoundException.java
│   │   ├── SettlementNotFoundException.java
│   │   ├── ExchangeRateNotFoundException.java
│   │   └── ReceivableAlreadySettledException.java
│   ├── port/
│   │   ├── in/                             # Interfaces que a app expõe (use cases)
│   │   │   ├── ManageAssignorUseCase.java
│   │   │   ├── ManageReceivableUseCase.java
│   │   │   ├── ExecuteSettlementUseCase.java
│   │   │   └── ManageExchangeRateUseCase.java
│   │   └── out/                            # Interfaces para repositórios externos
│   │       ├── AssignorRepositoryPort.java
│   │       ├── ReceivableRepositoryPort.java
│   │       ├── SettlementRepositoryPort.java
│   │       └── ExchangeRateRepositoryPort.java
│   └── service/pricing/                    # Strategy Pattern de precificação
│       ├── PricingStrategy.java            # Interface da estratégia
│       ├── DuplicataPricingStrategy.java   # Spread 1,5% a.m.
│       ├── PostDatedCheckPricingStrategy.java  # Spread 2,5% a.m.
│       └── PricingContext.java             # Seletor de estratégia por tipo
│
├── application/                            # ← Orquestração (sem JPA, sem HTTP)
│   ├── usecase/
│   │   ├── AssignorApplicationService.java
│   │   ├── ReceivableApplicationService.java
│   │   ├── SettlementApplicationService.java
│   │   └── ExchangeRateApplicationService.java
│   └── dto/
│       ├── request/                        # Java Records com validação @Valid
│       │   ├── CreateAssignorRequest.java
│       │   ├── UpdateAssignorRequest.java
│       │   ├── CreateReceivableRequest.java
│       │   ├── ExecuteSettlementRequest.java
│       │   └── CreateExchangeRateRequest.java
│       └── response/                       # Java Records imutáveis
│           ├── AssignorResponse.java
│           ├── ReceivableResponse.java
│           ├── SettlementResponse.java
│           ├── ExchangeRateResponse.java
│           ├── PresentValueResponse.java
│           └── SettlementStatementResponse.java
│
└── infrastructure/                         # ← Adapters Spring / JPA / REST
    ├── adapter/
    │   ├── in/rest/                        # Controllers (entrada HTTP)
    │   │   ├── AssignorController.java
    │   │   ├── ReceivableController.java
    │   │   ├── SettlementController.java
    │   │   ├── ExchangeRateController.java
    │   │   ├── ReportController.java       # Rota de relatório (2 camadas)
    │   │   └── GlobalExceptionHandler.java
    │   └── out/persistence/               # Adapters JPA (saída para BD)
    │       ├── entity/                    # Entidades com anotações @Entity
    │       ├── repository/                # Spring Data JPA + query nativa
    │       └── adapter/                   # Implementam as portas do domínio
    └── config/
        └── OpenApiConfig.java             # Configuração do Swagger
```

### Fluxo de uma Requisição

```
HTTP POST /api/v1/settlements
        │
        ▼
SettlementController          (Infrastructure — REST)
        │  chama use case via interface
        ▼
ExecuteSettlementUseCase      (Domain — Port/In)
        │  implementado por
        ▼
SettlementApplicationService  (Application — Use Case)
        │  1. findByIdWithLock() via ReceivableRepositoryPort
        │  2. receivable.markSettled()    ← regra de negócio no domínio
        │  3. PricingContext.strategyFor(type).calculatePresentValue()
        │  4. exchangeRateRepository.findByPair() se cross-currency
        │  5. settlementRepository.save()
        ▼
ReceivableRepositoryAdapter   (Infrastructure — JPA Adapter)
        │  SELECT FOR UPDATE (pessimistic lock)
        ▼
Banco de Dados                (H2 / Amazon RDS)
```

---

## Domínio do Negócio

### Entidades

#### `Assignor` — Cedente
Representa a empresa ou pessoa física que cede os recebíveis. Implementado como um POJO imutável com padrão `@Builder` e `@With` do Lombok, permitindo a criação de novas instâncias a partir de modificações sem mutação de estado.

```java
Assignor assignor = Assignor.builder()
    .id(UUID.randomUUID())
    .name("Empresa XYZ Ltda")
    .document("12345678000190")  // CNPJ
    .email("financeiro@xyz.com.br")
    .active(true)
    .build();
```

#### `Receivable` — Título
Representa o ativo financeiro (duplicata ou cheque). Contém a lógica de transição de estado diretamente na entidade, seguindo o princípio de **Domain Model Rico** (oposto ao Anemic Domain Model):

```java
// A entidade valida e transita o próprio estado
public Receivable markSettled() {
    if (isSettled() || isCancelled()) {
        throw new ReceivableAlreadySettledException(id, status);
    }
    return this.withStatus(SettlementStatus.SETTLED);
}
```

#### `Settlement` — Liquidação
Registro imutável que armazena todos os parâmetros utilizados no momento da liquidação: taxa base, valor presente calculado, taxa de câmbio utilizada e valor convertido.

#### `ExchangeRate` — Taxa de Câmbio
Par de moedas com taxa de conversão, data de atualização e fonte (`MANUAL` ou `MOCK_API`). A constraint única no banco de dados garante que só existe uma taxa ativa por par de moedas.

---

### Motor de Precificação (Strategy Pattern)

O **Strategy Pattern** é aplicado para desacoplar a regra de spread do algoritmo de cálculo. Cada tipo de recebível carrega um risco diferente, representado pelo spread mensal.

#### Fórmula

```
VP = VF / (1 + TaxaBase + Spread) ^ Prazo
```

Onde:
- **VP** = Valor Presente (quanto o cedente recebe hoje)
- **VF** = Valor de Face (valor nominal do título)
- **TaxaBase** = CDI / Selic mensal (informado na requisição)
- **Spread** = risco específico do tipo do recebível
- **Prazo** = prazo em meses até o vencimento

#### Spreads por Tipo

| Tipo | Enum | Spread | Justificativa |
|---|---|---|---|
| Duplicata (Nota Fiscal) | `DUPLICATA` | **1,5% a.m.** | Lastro em NF-e, menor risco de repúdio |
| Cheque Pré-datado | `POST_DATED_CHECK` | **2,5% a.m.** | Sem garantia real, maior risco de inadimplência |

#### Como adicionar um novo tipo

1. Adicionar o valor no enum `ReceivableType`
2. Criar a classe `NovoTipoPricingStrategy implements PricingStrategy`
3. Registrar no `PricingContext`:

```java
// PricingContext.java
static {
    STRATEGIES.put(ReceivableType.DUPLICATA,        new DuplicataPricingStrategy());
    STRATEGIES.put(ReceivableType.POST_DATED_CHECK,  new PostDatedCheckPricingStrategy());
    STRATEGIES.put(ReceivableType.NOVO_TIPO,         new NovoTipoPricingStrategy()); // ← aqui
}
```

#### Operação Cross-Currency

Quando o recebível tem moeda do ativo diferente da moeda de pagamento (ex: título em BRL, pagamento em USD), o valor presente é calculado em BRL primeiro e depois convertido:

```
VP_USD = (VF_BRL / (1 + taxa + spread)^prazo) × CotacaoBRL_USD
```

---

### Motor de Câmbio

O sistema suporta três origens de taxa de câmbio:

| Origem | Endpoint | Descrição |
|---|---|---|
| **Manual** | `POST /api/v1/exchange-rates` | Operador registra a taxa manualmente |
| **Atualização manual** | `PUT /api/v1/exchange-rates/{id}` | Operador atualiza uma taxa existente |
| **Mock externo** | `POST /api/v1/exchange-rates/sync-mock` | Simula integração com provedor externo |

O endpoint de mock simula um provedor externo e carrega automaticamente as taxas: `USD→BRL`, `EUR→BRL`, `BRL→USD`, `BRL→EUR`. Em produção, esse método seria substituído por uma chamada HTTP real (ex: Banco Central, Open Exchange Rates).

---

### Liquidação ACID

A liquidação é o processo mais crítico do sistema, pois envolve a transição irreversível de um recebível de `PENDING` para `SETTLED`. Dois problemas clássicos de concorrência são tratados:

#### Problema 1 — Double Settlement (Race Condition)
Sem proteção, dois threads simultâneos poderiam liquidar o mesmo recebível:

```
Thread A: findById(id) → status=PENDING ✓
Thread B: findById(id) → status=PENDING ✓
Thread A: save(SETTLED) ← primeiro a chegar
Thread B: save(SETTLED) ← também salva, criando liquidação duplicada ❌
```

#### Solução implementada

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public SettlementResponse execute(ExecuteSettlementRequest request) {

    // SELECT ... FOR UPDATE — bloqueia o row no banco
    Receivable receivable = receivableRepository.findByIdWithLock(receivableId);

    // Validação no domínio — lança exceção se já liquidado
    Receivable settled = receivable.markSettled();

    // ... cálculo do VP e câmbio ...

    settlementRepository.save(settlement);  // commit atômico
}
```

```java
// ReceivableJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)   // → gera SELECT FOR UPDATE
@Query("SELECT r FROM ReceivableJpaEntity r WHERE r.id = :id")
Optional<ReceivableJpaEntity> findByIdWithLock(@Param("id") UUID id);
```

O isolamento `SERIALIZABLE` combinado com o lock pessimista garante que:
1. Nenhum outro thread lê o recebível enquanto ele está sendo processado
2. A exceção `ReceivableAlreadySettledException` é lançada com HTTP 409 caso a corrida aconteça

---

## API REST

### Autenticação e Autorização

A API usa **JWT stateless** (Spring Security). Todos os endpoints sob `/api/v1/**` exigem um
token válido, exceto `/api/v1/auth/login`.

#### Obtendo um token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

Envie o token em cada requisição subsequente:

```bash
curl http://localhost:8080/api/v1/assignors \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### Roles

| Role | Permissões |
|---|---|
| `ADMIN` | Acesso total, incluindo cadastro/edição/desativação de cedentes (PII) e gestão de taxas de câmbio |
| `OPERATOR` | Cadastra recebíveis e executa liquidações (movimentação financeira) + leitura |
| `VIEWER` | Somente leitura (consultas e relatórios) |

Usuários são armazenados na tabela `users` (senha com hash BCrypt). Em ambiente de
desenvolvimento (perfil `!prod`, H2), o `DevUserSeeder` cria automaticamente `admin/admin123`,
`operator/operator123` e `viewer/viewer123` — **nunca use essas credenciais em produção**.
Em produção, crie usuários manualmente (ex.: via `psql` com `crypt(senha, gen_salt('bf'))`
usando a extensão `pgcrypto`, compatível com `BCryptPasswordEncoder`).

Configuração via variáveis de ambiente (ver `.env.example`):

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | ✅ (em `prod`) | Segredo HMAC-SHA256 para assinar os tokens (≥ 32 bytes aleatórios) |
| `JWT_EXPIRATION_MS` | — | Validade do token em ms (padrão `3600000` = 1h) |

### Endpoints

#### Cedentes (`/api/v1/assignors`)

| Método | Endpoint | Descrição | Role mínima | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/assignors` | Cadastrar cedente | `ADMIN` | `201 Created` |
| `GET` | `/api/v1/assignors` | Listar todos | autenticado | `200 OK` |
| `GET` | `/api/v1/assignors/{id}` | Buscar por ID | autenticado | `200 OK` |
| `PATCH` | `/api/v1/assignors/{id}` | Atualizar nome/email | `ADMIN` | `200 OK` |
| `DELETE` | `/api/v1/assignors/{id}` | Desativar (soft delete) | `ADMIN` | `204 No Content` |

#### Recebíveis (`/api/v1/receivables`)

| Método | Endpoint | Descrição | Role mínima | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/receivables` | Cadastrar título | `ADMIN`\|`OPERATOR` | `201 Created` |
| `GET` | `/api/v1/receivables` | Listar todos | autenticado | `200 OK` |
| `GET` | `/api/v1/receivables/{id}` | Buscar por ID | autenticado | `200 OK` |
| `GET` | `/api/v1/receivables/by-assignor/{id}` | Títulos por cedente | autenticado | `200 OK` |
| `GET` | `/api/v1/receivables/{id}/simulate?baseRate=0.01` | **Simular VP sem liquidar** | autenticado | `200 OK` |

#### Liquidações (`/api/v1/settlements`)

| Método | Endpoint | Descrição | Role mínima | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/settlements` | **Executar liquidação (ACID)** | `ADMIN`\|`OPERATOR` | `201 Created` |
| `GET` | `/api/v1/settlements/{id}` | Buscar por ID | autenticado | `200 OK` |
| `GET` | `/api/v1/settlements/by-receivable/{id}` | Por título | autenticado | `200 OK` |

#### Taxas de Câmbio (`/api/v1/exchange-rates`)

| Método | Endpoint | Descrição | Role mínima | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/exchange-rates` | Cadastrar taxa manual | `ADMIN` | `201 Created` |
| `GET` | `/api/v1/exchange-rates` | Listar todas | autenticado | `200 OK` |
| `GET` | `/api/v1/exchange-rates/{id}` | Buscar por ID | autenticado | `200 OK` |
| `GET` | `/api/v1/exchange-rates/pair?from=USD&to=BRL` | Buscar par | autenticado | `200 OK` |
| `PUT` | `/api/v1/exchange-rates/{id}` | Atualizar taxa | `ADMIN` | `200 OK` |
| `POST` | `/api/v1/exchange-rates/sync-mock` | Sincronizar mock externo | `ADMIN` | `200 OK` |

#### Relatórios (`/api/v1/reports`)

| Método | Endpoint | Descrição | Role mínima | Status |
|---|---|---|---|---|
| `GET` | `/api/v1/reports/settlement-statement` | **Extrato de liquidações** | autenticado | `200 OK` |

Parâmetros do extrato (todos opcionais):

| Parâmetro | Tipo | Exemplo |
|---|---|---|
| `startDate` | `ISO 8601` | `2024-01-01T00:00:00` |
| `endDate` | `ISO 8601` | `2024-12-31T23:59:59` |
| `assignorId` | `UUID` | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| `paymentCurrency` | `BRL\|USD\|EUR` | `BRL` |
| `page` | `int` | `0` |
| `size` | `int` | `20` |

### Documentação Swagger

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

A interface Swagger permite executar todos os endpoints interativamente, visualizar schemas de request/response e os códigos HTTP possíveis para cada operação.

---

## Relatórios Analíticos (Camada de 2 Níveis)

Os relatórios foram projetados para bypassar intencionalmente a camada de aplicação e domínio, seguindo a **arquitetura de 2 camadas para consultas**:

```
HTTP GET /api/v1/reports/settlement-statement
        │
        ▼
ReportController              (Infraestrutura — REST)
        │  injeta diretamente
        ▼
SettlementStatementQueryRepository  (Infraestrutura — SQL)
        │  Native SQL com LIMIT/OFFSET
        ▼
Banco de Dados
```

**Motivação:** queries analíticas com grandes volumes de dados, múltiplos JOINs e filtros dinâmicos não se beneficiam das abstrações ORM e passam por uma camada extra desnecessariamente. Com SQL nativo temos controle total sobre plano de execução e índices.

```sql
SELECT
    s.id, s.settled_at,
    a.id, a.name, a.document,
    r.id, r.type, r.face_value, r.asset_currency,
    s.base_rate, s.present_value,
    s.exchange_rate_used, s.present_value_converted, s.payment_currency
FROM settlements s
JOIN receivables r ON s.receivable_id = r.id
JOIN assignors  a ON r.assignor_id    = a.id
WHERE (:startDate       IS NULL OR s.settled_at      >= :startDate)
  AND (:endDate         IS NULL OR s.settled_at      <= :endDate)
  AND (:assignorId      IS NULL OR CAST(a.id AS VARCHAR) = :assignorId)
  AND (:paymentCurrency IS NULL OR s.payment_currency = :paymentCurrency)
ORDER BY s.settled_at DESC
LIMIT :size OFFSET :page * :size
```

Os índices definidos nas entidades JPA cobrem os campos de filtro mais comuns:

```
idx_settlements_settled_at       → filtro por período
idx_settlements_payment_currency → filtro por moeda
idx_settlements_receivable       → join com receivables
idx_receivables_assignor         → join com assignors
```

---

## Banco de Dados

### Modelo de Dados

```
assignors
├── id             UUID PK
├── name           VARCHAR(150)
├── document       VARCHAR(14) UNIQUE   ← CPF ou CNPJ
├── email          VARCHAR(150)
├── active         BOOLEAN
├── created_at     TIMESTAMP
└── updated_at     TIMESTAMP

receivables
├── id             UUID PK
├── assignor_id    UUID FK → assignors
├── type           VARCHAR(30)          ← DUPLICATA | POST_DATED_CHECK
├── face_value     DECIMAL(19,4)
├── asset_currency VARCHAR(3)           ← BRL | USD | EUR
├── payment_currency VARCHAR(3)
├── maturity_date  DATE
├── term_months    INT
├── status         VARCHAR(20)          ← PENDING | SETTLED | CANCELLED
└── created_at     TIMESTAMP

settlements
├── id                     UUID PK
├── receivable_id          UUID FK → receivables
├── base_rate              DECIMAL(19,10)
├── present_value          DECIMAL(19,10)
├── exchange_rate_used     DECIMAL(19,10)  ← NULL se mesma moeda
├── present_value_converted DECIMAL(19,10)
├── payment_currency       VARCHAR(3)
└── settled_at             TIMESTAMP

exchange_rates
├── id            UUID PK
├── from_currency VARCHAR(3)
├── to_currency   VARCHAR(3)
├── rate          DECIMAL(19,10)
├── source        VARCHAR(20)   ← MANUAL | MOCK_API
└── updated_at    TIMESTAMP
     UNIQUE (from_currency, to_currency)

users
├── id            UUID PK
├── username      VARCHAR(100) UNIQUE
├── password      VARCHAR(100)  ← hash BCrypt, nunca texto plano
├── role          VARCHAR(20)   ← ADMIN | OPERATOR | VIEWER
├── enabled       BOOLEAN
└── created_at    TIMESTAMP
```

### Perfil de Desenvolvimento (H2)

Ativo por padrão. Não requer nenhuma instalação externa.

```yaml
# application.yml (padrão)
spring:
  datasource:
    url: jdbc:h2:mem:creditdb
  jpa:
    hibernate:
      ddl-auto: create-drop  # schema criado na inicialização, removido ao encerrar
  h2:
    console:
      enabled: true
      path: /h2-console
```

Acesse o console H2 em `http://localhost:8080/h2-console`:
- **JDBC URL:** `jdbc:h2:mem:creditdb`
- **User:** `sa` | **Password:** *(vazio)*

### Perfil de Produção (AWS RDS)

O datasource de produção **não usa senha estática nem token exportado manualmente**. Um `DataSource` customizado (`RdsIamDataSourceConfig` / `IamAuthPostgresDataSource`) gera um token de autenticação IAM novo — via AWS SDK — a cada conexão física aberta pelo pool HikariCP, respeitando a validade de 15 minutos do token.

```java
// RdsIamDataSourceConfig.java (ativo apenas no perfil "prod")
@Bean
public DataSource dataSource() {
    RdsUtilities rdsUtilities = RdsUtilities.builder().region(Region.of(awsRegion)).build();
    DataSource iamAuthDataSource = new IamAuthPostgresDataSource(
            rdsUtilities, rdsHost, dbPort, dbName, dbUsername, sslMode);

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDataSource(iamAuthDataSource); // Hikari chama getConnection() a cada nova conexão física
    hikariConfig.setMaxLifetime(840_000);          // < 15 min, força reautenticação com token novo
    return new HikariDataSource(hikariConfig);
}
```

Ative com: `--spring.profiles.active=prod`

#### Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `RDSHOST` | ✅ | — | Endpoint RDS |
| `DB_USERNAME` | — | `postgres` | Usuário do banco (precisa da role `rds_iam`) |
| `DB_PORT` | — | `5432` | Porta do banco |
| `DB_NAME` | — | `postgres` | Nome do banco |
| `DB_SSL_MODE` | — | `require` | Modo SSL JDBC (`require` para RDS) |
| `AWS_REGION` | — | `sa-east-1` | Região do RDS, usada para assinar o token IAM |
| `DB_POOL_MAX` | — | `10` | Máximo de conexões HikariCP |
| `DB_POOL_MIN` | — | `2` | Mínimo de conexões ociosas HikariCP |

As credenciais AWS usadas para **assinar** o token (não a senha do banco) vêm da cadeia padrão do AWS SDK — IAM role da instância/ECS/EKS, ou `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` como fallback local. Nunca fixe essas credenciais no código ou em arquivos versionados.

#### Pré-requisitos no RDS

1. Crie uma instância **Amazon RDS** (versão 14+) com **IAM database authentication habilitado**
2. Use o banco `postgres` (padrão) ou crie um banco próprio e ajuste `DB_NAME`
3. Conceda a role IAM ao usuário do banco: `GRANT rds_iam TO postgres;`
4. Anexe ao principal IAM que roda a aplicação (role do ECS/EC2/EKS) uma policy permitindo `rds-db:connect` no ARN do usuário/instância
5. Configure o **Security Group** para permitir acesso na porta `5432` a partir do ECS/EC2 onde a API roda
6. Anote o **endpoint** (ex: `database-1.xxxx.sa-east-1.rds.amazonaws.com`)


---

## Como Executar

### Pré-requisitos

- **Java 17+** instalado e configurado no `JAVA_HOME`
- **Maven 3.9+** — utilize o Maven embutido do IntelliJ IDEA (caso `mvn` não esteja no PATH do sistema)
- Nenhum banco de dados externo necessário para o perfil `default`

> **Nota sobre o Maven:** Se o comando `mvn` não for reconhecido no terminal, utilize o Maven embutido do IntelliJ IDEA. Exemplo no Windows:
> ```
> "C:\Program Files\JetBrains\IntelliJ IDEA <versão>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" <goal>
> ```

### Executando Localmente

```bash
# 1. Clone o repositório
git clone https://github.com/fernandespaz/mcc-credit-assignment-api.git
cd srm-mcc-credit-assignment-api

# 2. Compile e execute os testes
mvn clean test

# 3. Execute a aplicação
mvn spring-boot:run
```

A API estará disponível em:

| Recurso | URL |
|---|---|
| API REST | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

#### Acessando o H2 Console

Ao abrir `http://localhost:8080/h2-console`, preencha o formulário de login com os seguintes dados:

| Campo | Valor |
|---|---|
| **Driver Class** | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:mem:creditdb` |
| **User Name** | `SA` |
| **Password** | *(deixar em branco)* |

> ⚠️ **Atenção:** O H2 Console abre com a JDBC URL padrão (`jdbc:h2:~/test`). Certifique-se de substituí-la por `jdbc:h2:mem:creditdb` antes de conectar, caso contrário ocorrerá erro de banco não encontrado.

### Executando em Produção (JAR direto)

```bash
# Configure as variáveis de ambiente do RDS (sem token/senha — geração automática via AWS SDK)
export RDSHOST="database-1.cr0km4kiuprv.sa-east-1.rds.amazonaws.com"
export DB_USERNAME=postgres
export DB_NAME=postgres
export DB_SSL_MODE=require
export AWS_REGION=sa-east-1
# Se não houver IAM role anexada ao host (ex: teste local), exporte também:
# export AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_SESSION_TOKEN=...

# Gere o JAR
mvn clean package -DskipTests

# Execute com perfil de produção
java -jar target/srm-mcc-credit-assignment-api-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=prod
```

> **Verificação manual da conexão (opcional, fora da aplicação):**
> ```bash
> psql "host=$RDSHOST port=5432 dbname=postgres user=postgres sslmode=require password=$(aws rds generate-db-auth-token --hostname $RDSHOST --port 5432 --username postgres --region sa-east-1)"
> ```

### Executando em Docker com AWS RDS

```bash
# 1. Copie o arquivo de exemplo e preencha com os dados do RDS
cp .env.example .env
# edite o .env com o endpoint do RDS (e credenciais AWS apenas se não houver IAM role)

# 2. Suba o container da API
docker compose up --build

# Ou passe as variáveis inline (sem .env)
RDSHOST=database-1.cr0km4kiuprv.sa-east-1.rds.amazonaws.com \
DB_USERNAME=postgres \
DB_NAME=postgres \
AWS_REGION=sa-east-1 \
docker compose up --build
```


---

## Exemplos de Uso

### 1. Cadastrar um Cedente

```bash
curl -X POST http://localhost:8080/api/v1/assignors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Empresa XYZ Ltda",
    "document": "12345678000190",
    "email": "financeiro@xyz.com.br"
  }'
```

### 2. Registrar uma Duplicata

```bash
curl -X POST http://localhost:8080/api/v1/receivables \
  -H "Content-Type: application/json" \
  -d '{
    "assignorId": "<uuid-do-cedente>",
    "type": "DUPLICATA",
    "faceValue": 10000.00,
    "assetCurrency": "BRL",
    "paymentCurrency": "BRL",
    "maturityDate": "2024-06-30",
    "termMonths": 3
  }'
```

### 3. Simular o Valor Presente (sem liquidar)

```bash
# Com taxa base CDI de 1,07% a.m. e spread de 1,5% → taxa total 2,57% a.m.
# VP = 10.000 / (1 + 0.0107 + 0.015)^3 = 10.000 / 1.0257^3 ≈ R$ 9.287,53
curl "http://localhost:8080/api/v1/receivables/<uuid>/simulate?baseRate=0.0107"
```

### 4. Executar a Liquidação

```bash
curl -X POST http://localhost:8080/api/v1/settlements \
  -H "Content-Type: application/json" \
  -d '{
    "receivableId": "<uuid-do-recebivel>",
    "baseRate": 0.0107
  }'
```

### 5. Liquidação Cross-Currency (BRL → USD)

```bash
# Primeiro registre a taxa BRL→USD
curl -X POST http://localhost:8080/api/v1/exchange-rates \
  -H "Content-Type: application/json" \
  -d '{"fromCurrency": "BRL", "toCurrency": "USD", "rate": 0.1917}'

# Crie o recebível com moedas diferentes
curl -X POST http://localhost:8080/api/v1/receivables \
  -H "Content-Type: application/json" \
  -d '{
    "assignorId": "<uuid>",
    "type": "POST_DATED_CHECK",
    "faceValue": 5000.00,
    "assetCurrency": "BRL",
    "paymentCurrency": "USD",
    "maturityDate": "2024-09-30",
    "termMonths": 6
  }'

# Ao liquidar, o sistema calcula VP em BRL e converte para USD automaticamente
```

### 6. Consultar Extrato de Liquidações

```bash
curl "http://localhost:8080/api/v1/reports/settlement-statement\
?startDate=2024-01-01T00:00:00\
&endDate=2024-12-31T23:59:59\
&paymentCurrency=BRL\
&page=0&size=20"
```

---

## Decisões Técnicas

### Entidades de Domínio como POJOs Imutáveis

As entidades do domínio (`Assignor`, `Receivable`, `Settlement`, `ExchangeRate`) são POJOs puros, sem nenhuma anotação de framework. Isso garante:
- **Testabilidade:** podem ser instanciadas sem Spring Context
- **Portabilidade:** independentes de JPA, Spring ou qualquer infraestrutura
- **Imutabilidade:** uso de `@Builder` + `@With` para criar cópias modificadas sem alterar o estado original

As anotações `@Entity` e `@Table` ficam somente nas **entidades JPA de infraestrutura** (`*JpaEntity`), separadas das entidades de domínio.

### Records Java para DTOs

Todos os DTOs de request e response são `record`s Java (Java 16+), garantindo:
- Imutabilidade por padrão
- `equals`, `hashCode` e `toString` gerados automaticamente
- Código conciso sem boilerplate

### `BigDecimal` para Valores Financeiros

Todos os cálculos monetários usam `BigDecimal` com `MathContext.DECIMAL128` e `RoundingMode.HALF_UP`, evitando os problemas de precisão de `double` e `float` em operações financeiras.

### Relatórios com 2 Camadas

A decisão de fazer o `ReportController` chamar diretamente o `SettlementStatementQueryRepository` (sem passar pelo domínio) é intencional e justificada por:
- Queries analíticas são puramente de leitura, não executam lógica de negócio
- SQL nativo oferece melhor performance e controle de plano de execução
- Evita o overhead de hidratar entidades de domínio apenas para serializar em JSON

---

## Tratamento de Erros

Todos os erros são capturados pelo `GlobalExceptionHandler` e retornados no seguinte formato:

```json
{
  "status": 404,
  "message": "Receivable not found with id: 3fa85f64-...",
  "timestamp": "2024-08-11T15:32:01.123"
}
```

| Exceção de Domínio | HTTP Status | Cenário |
|---|---|---|
| `AssignorNotFoundException` | `404 Not Found` | Cedente não encontrado |
| `ReceivableNotFoundException` | `404 Not Found` | Título não encontrado |
| `SettlementNotFoundException` | `404 Not Found` | Liquidação não encontrada |
| `ExchangeRateNotFoundException` | `404 Not Found` | Par de moedas sem cotação cadastrada |
| `ReceivableAlreadySettledException` | `409 Conflict` | Tentativa de liquidar título já liquidado |
| `DomainException` (genérica) | `422 Unprocessable Entity` | Violação de regra de negócio |
| `MethodArgumentNotValidException` | `400 Bad Request` | Payload com campos inválidos (`@Valid`) |
| `Exception` (inesperada) | `500 Internal Server Error` | Erros não previstos (logados) |

---

## ADR — Architecture Decision Records

Os ADRs documentam o *raciocínio* por trás de decisões arquiteturais significativas. Uma vez registrados, servem de referência para qualquer engenheiro que questione "por que foi feito assim?".

> Formato: **Contexto → Alternativas Consideradas → Decisão → Consequências**

---

### ADR-001: SQL Relacional vs NoSQL

**Data:** 2024-08  
**Status:** Aceito

#### Contexto
O sistema precisa persistir liquidações financeiras que envolvem múltiplas entidades relacionadas (cedente → recebível → liquidação), com regras de integridade fortes e suporte a queries analíticas complexas com filtros ad hoc.

#### Alternativas Consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Amazon RDS (escolhido)** | ACID nativo, JOINs eficientes, suporte a row-level locking (`SELECT FOR UPDATE`), maturidade comprovada em fintech, gerenciado pela AWS | Escalabilidade horizontal mais complexa |
| MongoDB | Schema flexível, escala horizontal simples | Sem JOINs nativos, transações multi-documento mais limitadas, menos adequado para relatórios relacionais |
| DynamoDB | Escala massiva, latência baixa | Sem JOINs, modelo de acesso rígido, queries analíticas exigem exportação para Redshift ou Athena |
| Redis | Latência microsegundos | Não é um banco primário, sem durabilidade garantida por padrão |

#### Decisão
**Amazon RDS** com Spring Data JPA para escrita e SQL nativo para leitura analítica.

#### Consequências
- ✅ Propriedades ACID garantidas sem custo adicional de implementação
- ✅ `SELECT FOR UPDATE` resolve race conditions de liquidação de forma nativa
- ✅ Queries analíticas com filtros dinâmicos e paginação são simples e performáticas
- ✅ Infraestrutura gerenciada pela AWS (backups, failover, patches automáticos)
- ⚠️ Em escala (>10M registros), particionamento por data de `settled_at` será necessário (ver ADR de alta escala)

---

### ADR-002: Monolito Modular vs Microserviços

**Data:** 2024-08  
**Status:** Aceito

#### Contexto
O sistema possui quatro subdomínios identificados: Cedentes, Recebíveis, Liquidações e Câmbio. A equipe inicial é pequena e o volume de tráfego ainda é baixo.

#### Alternativas Consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Monolito Modular (escolhido)** | Deploy simples, transações locais, fácil refatoração, sem overhead de rede | Risco de acoplamento ao crescer; deploy único |
| Microserviços desde o início | Escala independente por serviço, isolamento de falhas | Overhead enorme: service discovery, distributed tracing, sagas, eventual consistency, infra complexa |
| Serverless (Lambda) | Escala automática, custo por uso | Cold start afeta latência financeira, limites de execução, difícil debugging |

#### Decisão
**Monolito Modular** com arquitetura hexagonal. Os módulos são separados por fronteiras de domínio claras (ports & adapters), o que torna a **extração futura para microserviços cirúrgica** — basta mover um pacote e substituir as chamadas locais por HTTP/gRPC.

#### Consequências
- ✅ Time-to-market muito menor
- ✅ Transações ACID locais sem necessidade de Saga Pattern
- ✅ Debugging e observabilidade mais simples
- ⚠️ Em cenário de 1M tx/min (ver seção de alta escala), a separação dos módulos de Liquidação e Relatórios em serviços independentes seria o próximo passo natural

---

### ADR-003: Arquitetura Hexagonal vs MVC Tradicional

**Data:** 2024-08  
**Status:** Aceito

#### Contexto
O domínio financeiro tem regras de negócio complexas (precificação, validações de estado, conversão de moeda) que precisam ser testadas isoladamente, sem depender de banco de dados ou framework.

#### Alternativas Consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Hexagonal / Ports & Adapters (escolhido)** | Domínio isolado, testável sem Spring, troca de infra sem impacto no negócio | Mais arquivos, curva de aprendizado para novos devs |
| MVC Tradicional (Controller → Service → Repository) | Familiar, menos código inicial | Lógica de negócio tende a vazar para serviços ou controllers; JPA polui o domínio |
| CQRS puro desde o início | Separação clara de leitura/escrita, escala independente | Complexidade excessiva para o estágio atual do sistema |

#### Decisão
**Arquitetura hexagonal simplificada** com três camadas: Domain, Application e Infrastructure. O relatório utiliza apenas duas camadas (Controller → SQL) como exceção justificada por ser read-only e não envolver lógica de negócio.

#### Consequências
- ✅ Entidades de domínio são POJOs puros — testáveis com `new Receivable()` sem Spring
- ✅ Troca de banco (H2 → Amazon RDS → outro) exige alteração somente nos adapters
- ✅ Pricing Strategy pode ser testada unitariamente com valores conhecidos
- ⚠️ Adapters de persistência exigem mapeamento manual entre entidades de domínio e entidades JPA

---

### ADR-004: Lock Pessimista vs Otimista para Liquidações

**Data:** 2024-08  
**Status:** Aceito

#### Contexto
Uma liquidação é uma operação financeira irreversível. Dois processos simultâneos tentando liquidar o mesmo recebível causariam **double settlement** — um erro crítico que representa perda financeira real.

#### Alternativas Consideradas

| Opção | Mecanismo | Adequado? |
|---|---|---|
| **Lock Pessimista (escolhido)** | `SELECT FOR UPDATE` — bloqueia o row no banco durante a transação | ✅ Ideal para operações críticas de baixa frequência, conflito esperado |
| Lock Otimista | `@Version` — detecta conflito na hora do `UPDATE`, lança exceção | ⚠️ Exige retry na aplicação; aceitável quando conflito é raro |
| Idempotência por chave única | Constraint `UNIQUE` no banco para o par `(receivable_id)` em settlements | ✅ Complementar, mas não substitui o lock |
| Fila de mensagens (serialização) | Liquidações processadas sequencialmente via Kafka | ✅ Solução ideal em alta escala, mas overkill para o estágio atual |

#### Decisão
**Lock pessimista** (`LockModeType.PESSIMISTIC_WRITE`) combinado com isolamento `SERIALIZABLE`. O lock pessimista é a escolha correta aqui porque:
1. O conflito é altamente prejudicial (não pode falhar silenciosamente)
2. A operação de liquidação é rápida (< 100ms), então o tempo de bloqueio é mínimo
3. A frequência de tentativas simultâneas no mesmo recebível é baixa

#### Consequências
- ✅ Garantia matemática de que não há double settlement
- ✅ A exceção `ReceivableAlreadySettledException` (HTTP 409) é clara para o cliente da API
- ⚠️ Em alta escala, o lock pessimista é um gargalo — a solução é serializar via Kafka (ver EDA)

---

### ADR-005: Strategy Pattern vs Configuração por Banco de Dados

**Data:** 2024-08  
**Status:** Aceito

#### Contexto
Diferentes tipos de recebível carregam diferentes spreads de risco. A regra de negócio poderia ser armazenada no banco ("para DUPLICATA, spread = 0.015") ou codificada como lógica.

#### Alternativas Consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Strategy Pattern em código (escolhido)** | Type-safe, testável, compilação garante cobertura de todos os tipos | Requer deploy para adicionar novo tipo |
| Configuração em banco de dados | Adicionar tipo sem deploy | Regras de negócio fora do código, difícil de versionar e auditar, risco de divergência |
| `switch/if-else` inline | Simples para poucos tipos | Viola Open/Closed Principle; difícil de escalar e testar isoladamente |

#### Decisão
**Strategy Pattern** com `PricingContext` como seletor. A adição de um novo tipo de recebível requer: (1) novo valor no enum, (2) nova classe de estratégia, (3) registro no `PricingContext` — todos auditados via código e cobertos por testes.

#### Consequências
- ✅ Cada estratégia pode ser testada com `assertThat(new DuplicataPricingStrategy().calculatePresentValue(...)).isEqualTo(...)`
- ✅ Open/Closed Principle: aberto para extensão, fechado para modificação
- ✅ Spread é rastreável em git history
- ⚠️ Spreads dinâmicos por cliente/contrato exigiriam uma evolução para Strategy com parâmetros configuráveis

---

## Design de Alta Escala — 1 Milhão de Transações/Minuto

### Contexto e Desafios

**1.000.000 tx/min ≈ 16.667 tx/s** de pico. O design atual (monolito + Amazon RDS single node) suporta aproximadamente **500–2.000 tx/s** com hardware moderno. Atingir a meta exige uma série de mudanças arquiteturais.

**Principais gargalos a resolver:**

| Gargalo | Impacto | Solução |
|---|---|---|
| Escritas serializadas no RDS (lock pessimista) | Throughput máximo ~2k tx/s por shard | Sharding + Kafka para serialização |
| Leitura de taxas de câmbio a cada liquidação | N queries desnecessárias | Cache distribuído (Redis) |
| Relatórios analíticos em banco transacional | Contention com escritas | Replicação leitura + OLAP separado |
| Instância única da aplicação | Single point of failure | Múltiplas réplicas + Load Balancer |

---

### Arquitetura Alvo

```
                          ┌─────────────────────┐
                          │    API Gateway /     │
                          │    Load Balancer     │
                          │  (Nginx / AWS ALB)   │
                          └──────────┬──────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                      ▼
   ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │  Settlement      │  │  Settlement      │  │  Settlement      │
   │  Service         │  │  Service         │  │  Service         │
   │  (Pod/Instance)  │  │  (Pod/Instance)  │  │  (Pod/Instance)  │
   └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │ publica evento
                                  ▼
                        ┌─────────────────────┐
                        │       Kafka          │
                        │  (3+ brokers)        │
                        │  Topic: settlements  │
                        │  Partições por       │
                        │  assignor_id hash    │
                        └──────────┬──────────┘
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
          ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
          │  Settlement  │ │  Notification│ │  Analytics   │
          │  Processor   │ │  Service     │ │  Consumer    │
          │  (Consumer)  │ │              │ │              │
          └──────┬───────┘ └──────────────┘ └──────┬───────┘
                 │                                  │
     ┌───────────▼──────────┐             ┌────────▼────────┐
     │   Amazon RDS         │             │   ClickHouse /  │
     │   (Sharded)          │             │   Redshift      │
     │   Shard 0: A–F       │             │   (OLAP)        │
     │   Shard 1: G–N       │             └─────────────────┘
     │   Shard 2: O–Z       │
     └──────────────────────┘
              ▲
     ┌────────┴────────┐
     │     Redis        │
     │  (Cache Layer)   │
     │  - Exchange Rates│
     │  - Assignors     │
     └─────────────────┘
```

---

### Caching

#### O que cachear?

| Dado | TTL | Estratégia | Justificativa |
|---|---|---|---|
| Taxas de câmbio | 60 segundos | Cache-Aside | Mudam a cada minuto no mercado; lidas em toda liquidação |
| Dados do cedente (Assignor) | 5 minutos | Cache-Aside + Write-Through | Raramente mudam; lidos em toda criação de recebível |
| Resultados de relatórios | 30 segundos | Cache por hash de parâmetros | Relatórios idênticos são frequentes em dashboards |

#### Implementação com Redis

```java
// ExchangeRateApplicationService.java (adaptado para alta escala)
@Cacheable(value = "exchange-rates", key = "#from + '_' + #to")
public ExchangeRateResponse findByPair(Currency from, Currency to) {
    return exchangeRateRepository.findByPair(from, to)
            .map(this::toResponse)
            .orElseThrow(() -> new ExchangeRateNotFoundException(from, to));
}

@CacheEvict(value = "exchange-rates", key = "#request.fromCurrency + '_' + #request.toCurrency")
public ExchangeRateResponse update(UUID id, CreateExchangeRateRequest request) {
    // ...
}
```

```yaml
# application-prod.yml (adição)
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:redis-cluster}
      port: 6379
      timeout: 100ms
      lettuce:
        pool:
          max-active: 50
```

#### Cache Stampede (Thundering Herd)
Para evitar que múltiplas instâncias reconstroem o cache simultaneamente após expiração, usar **probabilistic early expiration** ou **Lua script com lock distribuído** no Redis.

---

### Sharding e Particionamento

#### Estratégia de Sharding para Liquidações

Com 1M tx/min, a tabela `settlements` cresce ~1.5B registros/mês. A estratégia recomendada:

**1. Particionamento por data (Range Partitioning)** — nível RDS:
```sql
-- Tabela mãe
CREATE TABLE settlements (
    id UUID NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    ...
) PARTITION BY RANGE (settled_at);

-- Partições mensais criadas automaticamente via pg_partman
CREATE TABLE settlements_2024_01 PARTITION OF settlements
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

**Benefícios:**
- Queries por período (relatórios) tocam apenas 1–2 partições
- `DROP PARTITION` para arquivamento é instantâneo (sem `DELETE`)
- Índices menores por partição = melhor performance

**2. Sharding por `assignor_id` (Hash Sharding)** — nível aplicação:

Para liquidações acima de 10M/dia, distribuir em múltiplos nós RDS:
```
Shard = hash(assignor_id) % NUM_SHARDS

assignor_id hash % 3 == 0 → RDS Shard 0
assignor_id hash % 3 == 1 → RDS Shard 1
assignor_id hash % 3 == 2 → RDS Shard 2
```

O sharding por `assignor_id` garante que todas as liquidações de um cedente ficam no mesmo shard, preservando a possibilidade de queries analíticas por cedente sem fan-out.

---

### Consistência Eventual

Em alta escala, parte da consistência forte é trocada por **consistência eventual** onde o negócio permite.

#### O que pode ser eventualmente consistente?

| Operação | Consistência Atual | Alta Escala |
|---|---|---|
| Executar liquidação | **Forte** (ACID) | **Forte** — obrigatório, não negociável |
| Atualizar status do recebível | **Forte** (mesma tx) | **Eventual** — via evento após commit |
| Notificação ao cedente | N/A | **Eventual** — via Kafka consumer |
| Dashboard / relatórios | Forte (query direta) | **Eventual** — banco OLAP atualizado a cada 30s |
| Taxa de câmbio para precificação | Forte (query a cada tx) | **Eventual** — cache Redis com TTL 60s |

#### Padrão Outbox para Garantia de Entrega

Para garantir que eventos são publicados mesmo em caso de falha entre o commit do banco e a publicação no Kafka:

```
┌─────────────────────────────────────────────────┐
│  TRANSACTION                                     │
│  1. INSERT INTO settlements (...)                │
│  2. UPDATE receivables SET status = 'SETTLED'    │
│  3. INSERT INTO outbox (event_type, payload)  ←  │ atomicamente junto
└─────────────────────────────────────────────────┘
                          │
          ┌───────────────▼──────────────┐
          │   Outbox Poller (Debezium /  │
          │   scheduled job)             │
          │   SELECT * FROM outbox       │
          │   WHERE published = false    │
          └───────────────┬──────────────┘
                          │ publica
                          ▼
                        Kafka
```

Isso garante **exatamente-uma-vez** na publicação do evento, sem perda mesmo em falhas de rede ou crash da aplicação.

---

### Estimativa de Capacidade

| Métrica | Valor |
|---|---|
| Taxa de pico | 1.000.000 tx/min ≈ 16.667 tx/s |
| Tamanho médio de uma liquidação | ~500 bytes |
| Throughput de escrita | ~8 MB/s |
| Crescimento da tabela `settlements` | ~42 GB/dia |
| Instâncias da aplicação necessárias (8 cores) | ~12–16 pods |
| Shards RDS necessários | 4–8 (com particionamento por data) |
| Partições Kafka (topic `settlements`) | 64 (escalável) |
| Cache Redis — memória estimada (exchange rates) | < 1 MB (dados pequenos) |
| Cache Redis — memória estimada (assignors hot) | ~500 MB para 1M cedentes |

---

## Modelagem de Eventos — EDA

### Por que Event-Driven?

A arquitetura atual (síncrona, request-response) tem limitações em cenários de alta escala:

- **Acoplamento temporal:** o caller espera a resposta do callee
- **Gargalo no lock pessimista:** liquidações em série no mesmo banco
- **Sem auditoria natural:** não há registro imutável do que aconteceu e quando

A **Arquitetura Orientada a Eventos (EDA)** resolve esses problemas desacoplando produtores de consumidores e criando um log imutável de fatos do domínio.

---

### Eventos de Domínio

Cada evento representa um **fato que aconteceu** no domínio. São imutáveis, têm timestamp e versão.

```
ReceivableRegistered        → criação de novo título
ReceivableSettled           → liquidação executada com sucesso
ReceivableCancelled         → cancelamento de título
ExchangeRateUpdated         → nova cotação disponível
AssignorDeactivated         → cedente desativado
SettlementFailed            → liquidação falhou (ex: saldo insuficiente)
```

#### Schema de Evento (CloudEvents + envelope)

```json
{
  "specversion": "1.0",
  "id": "b4f9a3c1-7e2d-4f8a-9b1c-3e5f7a9c2d4e",
  "source": "credit-assignment-api/settlements",
  "type": "com.srm.mcc.credit.ReceivableSettled",
  "time": "2024-08-11T15:32:01.123Z",
  "datacontenttype": "application/json",
  "data": {
    "settlementId": "a1b2c3d4-...",
    "receivableId": "e5f6g7h8-...",
    "assignorId":   "i9j0k1l2-...",
    "faceValue":    10000.00,
    "presentValue": 9287.53,
    "currency":     "BRL",
    "baseRate":     0.0107,
    "spread":       0.015,
    "settledAt":    "2024-08-11T15:32:01Z"
  }
}
```

---

### Diagrama de Fluxo de Eventos

```
                    HTTP POST /settlements
                           │
                           ▼
                ┌──────────────────────┐
                │  Settlement Service  │
                │  (Producer)          │
                └──────────┬───────────┘
                           │
              ┌────────────▼────────────┐
              │    BEGIN TRANSACTION    │
              │  1. UPDATE receivable   │
              │     status → SETTLED    │
              │  2. INSERT settlement   │
              │  3. INSERT outbox event │
              │    COMMIT               │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │    Outbox Poller        │
              │   (Debezium CDC ou      │
              │    Scheduled Job)       │
              └────────────┬────────────┘
                           │ publica
                           ▼
              ┌────────────────────────────────────────┐
              │           Kafka                         │
              │   Topic: credit.receivable.settled      │
              │   Partição: hash(assignor_id) % 64      │
              └──────┬───────────┬──────────┬───────────┘
                     │           │          │
          ┌──────────▼──┐  ┌─────▼───┐  ┌──▼──────────────┐
          │ Notification│  │ Ledger  │  │ Analytics       │
          │ Service     │  │ Service │  │ Consumer        │
          │             │  │         │  │                 │
          │ Envia email/│  │ Debita  │  │ INSERT INTO     │
          │ webhook ao  │  │ posição │  │ ClickHouse      │
          │ cedente     │  │ no      │  │ (OLAP)          │
          │             │  │ balanço │  │                 │
          └─────────────┘  └─────────┘  └─────────────────┘
```

---

### Padrões Aplicados

#### 1. Event Sourcing (opcional — para auditoria total)
Em vez de persistir apenas o estado atual, persistir todos os eventos que levaram ao estado. O estado atual é a projeção (fold/reduce) de todos os eventos.

```
events table:
┌────────────────────────────────────────────────────────┐
│ aggregate_id │ event_type          │ payload │ seq │ ts │
│ recv-001     │ ReceivableRegistered│ {...}   │ 1   │ .. │
│ recv-001     │ ReceivableSettled   │ {...}   │ 2   │ .. │
└────────────────────────────────────────────────────────┘

Estado atual = fold(eventos) = { status: SETTLED, ... }
```

**Benefício:** auditoria completa, replay de eventos para corrigir bugs, time-travel queries.

#### 2. CQRS — Command Query Responsibility Segregation
Separar o modelo de escrita (Command) do modelo de leitura (Query):

```
Command Side:                    Query Side:
POST /settlements           →    GET /reports/settlement-statement
      │                                │
      ▼                                ▼
Settlement Aggregate          Read Model (projeção)
(estado consistente)          (desnormalizado, otimizado
      │                        para a query específica)
      │ evento                         ▲
      └────────────────────────────────┘
              (consumidor atualiza
               a projeção)
```

#### 3. Saga Pattern — para operações multi-serviço
Quando a liquidação envolver múltiplos serviços (ex: debitar conta do cessionário E creditar o cedente), usar o padrão **Saga Coreografado**:

```
Settlement Service → publica SettlementInitiated
      │
      ▼
Account Service (subscribe) → debita cessionário → publica AccountDebited
      │
      ▼
Assignor Payment Service (subscribe) → credita cedente → publica AssignorCredited
      │
      ▼
Settlement Service (subscribe) → confirma → publica SettlementCompleted

Em caso de falha em qualquer etapa → publica evento de compensação
(ex: AccountDebitReversed)
```

---

## IaC — Kubernetes Manifests

Os manifests abaixo representam o deployment da API no Kubernetes, prontos para uso em um cluster gerenciado (GKE, EKS, AKS).

### `k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: credit-assignment
  labels:
    app.kubernetes.io/part-of: srm-mcc
```

### `k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: credit-assignment-config
  namespace: credit-assignment
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SERVER_PORT: "8080"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"
  SPRING_JPA_SHOW_SQL: "false"
```

### `k8s/secret.yaml`

```yaml
# Em produção, use External Secrets Operator ou Vault — nunca valores em plain text
apiVersion: v1
kind: Secret
metadata:
  name: credit-assignment-secret
  namespace: credit-assignment
type: Opaque
stringData:
  DB_URL:      "jdbc:postgresql://<rds-endpoint>:5432/creditdb"
  DB_USERNAME: "credit_user"
  DB_PASSWORD: "SUBSTITUA_PELO_VALOR_REAL"
```

### `k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: credit-assignment-api
  namespace: credit-assignment
  labels:
    app: credit-assignment-api
    version: "1.0.0"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: credit-assignment-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0          # zero-downtime deploy
  template:
    metadata:
      labels:
        app: credit-assignment-api
    spec:
      containers:
        - name: api
          image: ghcr.io/srm-mcc/credit-assignment-api:1.0.0
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: credit-assignment-config
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: credit-assignment-secret
                  key: DB_URL
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: credit-assignment-secret
                  key: DB_USERNAME
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: credit-assignment-secret
                  key: DB_PASSWORD
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "2000m"
              memory: "1Gi"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
            failureThreshold: 3
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 40
            periodSeconds: 15
            failureThreshold: 5
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]  # graceful shutdown
      terminationGracePeriodSeconds: 60
```

### `k8s/service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: credit-assignment-api
  namespace: credit-assignment
spec:
  selector:
    app: credit-assignment-api
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
```

### `k8s/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: credit-assignment-ingress
  namespace: credit-assignment
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "1000"       # req/s por IP
    nginx.ingress.kubernetes.io/rate-limit-burst: "2000"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.srm-mcc.com.br
      secretName: srm-tls-secret
  rules:
    - host: api.srm-mcc.com.br
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: credit-assignment-api
                port:
                  number: 80
```

### `k8s/hpa.yaml` — Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: credit-assignment-hpa
  namespace: credit-assignment
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: credit-assignment-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 30    # reage rápido ao pico
      policies:
        - type: Pods
          value: 4
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300   # escala para baixo com calma
```

### Aplicando os manifests

```bash
# Aplicar toda a configuração
kubectl apply -f k8s/

# Verificar status do deploy
kubectl rollout status deployment/credit-assignment-api -n credit-assignment

# Verificar pods
kubectl get pods -n credit-assignment

# Ver logs em tempo real
kubectl logs -f -l app=credit-assignment-api -n credit-assignment

# Rollback em caso de problema
kubectl rollout undo deployment/credit-assignment-api -n credit-assignment
```
