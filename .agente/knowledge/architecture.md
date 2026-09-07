# Arquitetura — Hexagonal simplificada em 3 camadas

## Visão geral

O projeto segue **arquitetura hexagonal (ports & adapters)** simplificada em três camadas:

```
domain          → regras de negócio puras, sem dependências de frameworks (exceto Lombok)
application     → orquestração de casos de uso, DTOs de entrada/saída
infrastructure  → adapters de entrada (REST) e saída (JPA), configuração Spring
```

## Estrutura de pacotes (referência exata)

```
com.srm.mcc.credit
├── domain
│   ├── entity          → POJOs de domínio imutáveis (Assignor, Receivable, Settlement, ExchangeRate)
│   ├── enums            → Currency, ReceivableType, SettlementStatus, UserRole
│   ├── exception         → Exceptions de domínio (ex: ReceivableNotFoundException)
│   ├── port
│   │   ├── in            → Interfaces de use case (ex: ManageAssignorUseCase, ExecuteSettlementUseCase)
│   │   └── out            → Interfaces de repositório do ponto de vista do domínio (ex: ReceivableRepositoryPort)
│   └── service
│       └── pricing        → Strategy Pattern para cálculo de valor presente
├── application
│   ├── dto
│   │   ├── request        → Records de entrada (ex: CreateAssignorRequest)
│   │   └── response         → Records de saída (ex: AssignorResponse)
│   └── usecase             → Implementações dos use cases (ex: AssignorApplicationService)
└── infrastructure
    ├── adapter
    │   ├── in
    │   │   └── rest          → @RestController (ex: AssignorController)
    │   └── out
    │       └── persistence
    │           ├── entity      → @Entity JPA (ex: AssignorJpaEntity) — SEPARADO da entidade de domínio
    │           ├── repository  → Spring Data JpaRepository (ex: AssignorJpaRepository)
    │           └── adapter     → Implementa os *Port* do domínio usando os JpaRepository (ex: AssignorRepositoryAdapter)
    └── config               → Configurações Spring (SecurityConfig, RdsIamDataSourceConfig, OpenApiConfig)
        └── security          → JwtService, JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig
```

## Regras de dependência (não violar)

- `domain` **não pode** importar nada de `infrastructure` nem de `application`.
- `domain.entity` (POJOs) é **diferente** de
  `infrastructure.adapter.out.persistence.entity` (`@Entity` JPA). Nunca use a entidade JPA
  diretamente no domínio ou na camada de aplicação além do necessário para mapear no adapter.
- Um `@RestController` **nunca** deve chamar um `JpaRepository` diretamente. Ele depende de uma
  interface `domain.port.in.*UseCase`, implementada por um `application.usecase.*ApplicationService`.
- Uma `*ApplicationService` depende de `domain.port.out.*RepositoryPort` (interface), nunca de um
  `JpaRepository` concreto diretamente.
- O `*RepositoryAdapter` (em `infrastructure.adapter.out.persistence.adapter`) é a única classe
  que converte entre entidade de domínio e entidade JPA e delega ao `JpaRepository`.

### Exceção arquitetural conhecida e intencional

`ReportController` (`infrastructure/adapter/in/rest/ReportController.java`) **bypassa** as camadas
`domain`/`application` de propósito, chamando diretamente
`SettlementStatementQueryRepository` (SQL nativo). Isso é documentado no próprio código como
"2-layer path" para relatórios analíticos de alto volume. **Não replique esse padrão para outros
endpoints** sem justificativa equivalente (leitura analítica pesada, sem regra de negócio a
aplicar) — é uma exceção, não a norma.

## Convenções de nomenclatura

| Elemento | Convenção | Exemplo |
|---|---|---|
| Entidade de domínio | Substantivo simples | `Assignor`, `Receivable` |
| Entidade JPA | Sufixo `JpaEntity` | `AssignorJpaEntity` |
| Repositório Spring Data | Sufixo `JpaRepository` | `AssignorJpaRepository` |
| Port de saída (domínio) | Sufixo `RepositoryPort` | `AssignorRepositoryPort` |
| Adapter de persistência | Sufixo `RepositoryAdapter` | `AssignorRepositoryAdapter` |
| Port de entrada (use case) | Sufixo `UseCase` | `ManageAssignorUseCase` |
| Serviço de aplicação | Sufixo `ApplicationService` | `AssignorApplicationService` |
| Controller REST | Sufixo `Controller` | `AssignorController` |
| DTO de entrada | Sufixo `Request`, `record` | `CreateAssignorRequest` |
| DTO de saída | Sufixo `Response`, `record` | `AssignorResponse` |
| Exception de domínio | Sufixo `Exception`, estende `DomainException` | `AssignorNotFoundException` |

## Padrões de projeto em uso

- **Strategy Pattern** para precificação (`domain/service/pricing/`): cada `ReceivableType` tem
  uma `PricingStrategy` própria (`DuplicataPricingStrategy`, `PostDatedCheckPricingStrategy`),
  selecionada via `PricingContext.strategyFor(type)`. **Novo tipo de recebível = nova
  implementação de `PricingStrategy` + registro no `PricingContext`**, nunca `if/else`/`switch`
  espalhado pelo código de negócio.
- **Lock pessimista + isolamento SERIALIZABLE** em liquidações
  (`SettlementApplicationService.execute`, `@Transactional(isolation = Isolation.SERIALIZABLE)` +
  `receivableRepository.findByIdWithLock`) para evitar double-settlement. Não remova/enfraqueça
  esse controle sem entender completamente a race condition que ele previne (ver ADR-004 no
  README).
- **Lombok** (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) é usado
  consistentemente em entidades e serviços — siga o mesmo padrão em código novo.
- **Records Java** para todos os DTOs de request/response (imutáveis por padrão).

## Build e execução

```bash
# Maven não está no PATH neste ambiente Windows — use o wrapper do projeto ou o mvn do IntelliJ:
.\mvnw.cmd compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Perfis:
- **default** (sem flag) → H2 em memória, `ddl-auto: create-drop`, usuários de dev seedados
  automaticamente (`DevUserSeeder`).
- **prod** (`--spring.profiles.active=prod`) → AWS RDS PostgreSQL com autenticação IAM (token de
  15 min gerado dinamicamente, nunca hardcoded), `ddl-auto: update`. Requer `JWT_SECRET` obrigatório
  (sem fallback — falha rápido se ausente).
