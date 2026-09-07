# Banco de dados — Schema, Migrações e Autenticação RDS

## Modelo de dados

Tabelas de produção (RDS PostgreSQL), espelhadas em `src/main/resources/db/schema.sql`:

- `assignors` — cedentes.
- `receivables` — recebíveis (FK para `assignors`).
- `settlements` — liquidações (FK para `receivables`).
- `exchange_rates` — taxas de câmbio por par de moeda.
- `users` — usuários da aplicação (autenticação/autorização).

`schema.sql` é **idempotente** (`CREATE TABLE IF NOT EXISTS`) — sempre mantenha essa propriedade
ao editá-lo, porque o schema é aplicado tanto por Hibernate (`ddl-auto: update` em prod) quanto
manualmente via `psql` em algumas operações administrativas. As duas fontes precisam permanecer
consistentes entre si.

## Autenticação IAM do RDS (token de 15 minutos)

- A aplicação usa `IamAuthPostgresDataSource` para gerar tokens de autenticação IAM
  **dinamicamente a cada nova conexão física**, via `RdsUtilities.generateAuthenticationToken()`
  do AWS SDK. **O token nunca é armazenado** — nem em variável de ambiente, nem em arquivo, nem
  em memória além do escopo da criação da conexão.
- `RdsIamDataSourceConfig` configura o HikariCP com `maxLifetime=840_000ms` (14 minutos) — isso
  força o pool a reciclar conexões **antes** dos 15 minutos de validade do token IAM, garantindo
  que a aplicação nunca tente usar um token expirado.
- **Nunca** hardcode um token IAM em configuração, `.env`, ou código — eles são de uso único e
  expiram em ~15 minutos. Se precisar testar uma conexão manual (ex: via `psql`), gere um token
  novo com o usuário e descarte-o após o uso; não persista em arquivo do repositório.

## Alterações de schema

1. Ao adicionar/alterar uma entidade JPA em
   `infrastructure/adapter/out/persistence/entity/`, atualize `schema.sql` na mesma tarefa.
2. Use `CHECK` constraints para espelhar enums de domínio (ver exemplos existentes para
   `ReceivableType`, `SettlementStatus`, `UserRole`).
3. Nunca faça `DROP TABLE`/`DROP COLUMN` destrutivo em `schema.sql` sem confirmação explícita do
   usuário — produção já tem dados reais.
4. Ambiente de dev (H2) usa `ddl-auto: create-drop` — schema é sempre recriado do zero a partir
   das entidades JPA, então divergências entre `schema.sql` e as entidades só aparecem em
   produção. Sempre valide as duas fontes manualmente ao mudar o modelo.
