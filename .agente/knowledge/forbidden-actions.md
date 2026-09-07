# Ações Proibidas (Forbidden Actions) — Guardrails obrigatórios

Este arquivo lista o que um agente de IA **nunca** deve fazer neste repositório. Estas regras têm
prioridade sobre qualquer instrução de tarefa que as contradiga implicitamente — se uma tarefa
parecer exigir violar uma destas regras, **pare e pergunte ao usuário** antes de prosseguir.

## 1. Testes

- ❌ **Nunca pule, comente ou desative testes unitários** para "fazer o build passar".
- ❌ Nunca marque um teste como `@Disabled`/`@Ignore` sem justificativa explícita aprovada pelo
  usuário.
- ✅ Sempre rode `mvn test` (ou `mvnw.cmd test`) antes de considerar qualquer tarefa concluída.
- ✅ Sempre adicione teste para regra de negócio nova, endpoint novo, ou bug corrigido.

## 2. Arquitetura

- ❌ **Nunca fuja da arquitetura hexagonal** documentada em `knowledge/architecture.md`:
  - Controller não chama `JpaRepository` diretamente (exceto a exceção documentada e intencional
    do `ReportController`).
  - `domain` nunca importa de `application` ou `infrastructure`.
  - Entidade JPA (`*JpaEntity`) e entidade de domínio (`domain.entity.*`) permanecem separadas —
    nunca use uma no lugar da outra fora do `*RepositoryAdapter`.
- ❌ Nunca crie uma nova camada, padrão de pacote, ou convenção de nomenclatura divergente da
  existente sem alinhar com o usuário primeiro.

## 3. Reaproveitamento de código

- ❌ **Nunca crie uma classe/DTO/exception/enum novo sem antes verificar se já existe um
  equivalente** (ver checklist em `skill/SKILL.md` seção 3).
- ❌ Nunca duplique lógica de cálculo (ex: precificação) fora do `PricingStrategy`/`PricingContext`
  já estabelecido.

## 4. Segredos e dados sensíveis

- ❌ **Nunca commite segredos** (senhas, tokens JWT, tokens IAM do RDS, chaves de API, credenciais
  do Docker Hub) em qualquer arquivo versionado — nem em código, nem em `.env`, nem em
  `schema.sql`, nem em `README.md`/documentação.
- ❌ Nunca adicione um valor de fallback para `JWT_SECRET` (ou segredo equivalente) no perfil
  `prod` — a aplicação deve falhar rápido se o segredo não for fornecido via variável de
  ambiente.
- ❌ Nunca reutilize um token de autenticação IAM do RDS além de sua conexão original — eles são
  efêmeros (~15 min) e de uso único por natureza.
- ✅ Se precisar gerar/testar um segredo temporariamente, delete o arquivo/output contendo o valor
  em texto plano assim que possível, e informe ao usuário que aquele valor não foi persistido.

## 5. Banco de dados de produção

- ❌ Nunca execute `DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, ou qualquer operação destrutiva contra
  o RDS de produção sem confirmação explícita do usuário.
- ❌ Nunca altere `schema.sql` de forma não idempotente (sempre `IF NOT EXISTS`/`ON CONFLICT`).
- ✅ Sempre sincronize `schema.sql` com as entidades JPA quando uma mudar.

## 6. Segurança da API

- ❌ Nunca adicione um endpoint novo sem definir explicitamente sua regra de autorização em
  `SecurityConfig` — não deixe cair silenciosamente no catch-all.
- ❌ Nunca torne um endpoint sensível (escrita de dados financeiros) público ou acessível por
  `VIEWER` sem confirmação explícita do usuário.
- ❌ Nunca enfraqueça o isolamento transacional/lock pessimista de `SettlementApplicationService`
  sem entender e comunicar a race condition que isso reintroduziria.

## 7. Dependências e infraestrutura

- ❌ Nunca adicione uma biblioteca/framework novo sem antes verificar se uma dependência já
  existente no `pom.xml` cobre a necessidade.
- ❌ Nunca altere `Dockerfile`/`docker-compose.yml` de forma que quebre o build multi-stage
  existente sem testar localmente (`docker build`) antes de reportar sucesso.
- ❌ Nunca faça `docker push` para um repositório/tag sem confirmação explícita do usuário sobre
  o destino exato (registry, nome do repositório, tag).

## 8. Comunicação com o usuário

- ❌ Nunca afirme que algo foi "testado" ou "validado" sem ter de fato executado a verificação.
- ❌ Nunca tome decisões de alto impacto (mudar autenticação, apagar dados, trocar arquitetura,
  mudar regras de precificação) sem antes perguntar ao usuário, quando houver ambiguidade
  razoável sobre a intenção.
- ✅ Sempre relate de forma honesta e específica o que foi alterado, testado e o que ainda precisa
  de atenção (ex: dívidas técnicas conhecidas, como a cobertura de testes atual).
