# Segurança — Autenticação e Autorização

## Visão geral

A API usa **JWT stateless** (HMAC-SHA256/HS256, via `io.jsonwebtoken` 0.12.6). Implementado em
`infrastructure/config/security/`:

- `JwtService` — geração e validação de tokens.
- `CustomUserDetailsService` — carrega `UserJpaEntity` (que implementa `UserDetails` diretamente).
- `JwtAuthenticationFilter` — `OncePerRequestFilter` que lê o header `Authorization: Bearer <token>`.
- `SecurityConfig` — `SecurityFilterChain` com a matriz de autorização por endpoint.
- `AuthController` — único endpoint de login: `POST /api/v1/auth/login`.

## Matriz de autorização (não alterar sem avaliar impacto)

| Rota | Acesso |
|---|---|
| `/api/v1/auth/**` | Público |
| `/actuator/health/**` | Público (necessário para health check do ALB) |
| `/v3/api-docs/**`, `/swagger-ui/**` | Público |
| `/h2-console/**` | Público (apenas ambiente dev, H2) |
| Assignors POST/PATCH/DELETE | `ADMIN` |
| Exchange-rates POST/PUT | `ADMIN` |
| `/actuator/**` (exceto health) | `ADMIN` |
| Receivables POST, Settlements POST | `ADMIN` ou `OPERATOR` |
| Qualquer outro `/api/v1/**` (GET, reports) | Qualquer usuário autenticado |
| **Qualquer rota não listada, incluindo `/`** | `authenticated()` — **retorna 401** |

⚠️ **Importante**: `/` (raiz) **não é pública** — está sob `anyRequest().authenticated()`. Isso já
causou quebra do health check do ALB (que apontava para `/`) — foi corrigido apontando o health
check para `/actuator/health`. Se você adicionar um novo endpoint, **decida explicitamente** sua
regra de acesso em `SecurityConfig` — não deixe cair no catch-all sem avaliar se deveria ser
público.

## Segredos

- **Dev** (`application.yml`): `security.jwt.secret` tem um fallback hardcoded **inseguro**
  (`dev-only-insecure-secret-change-me-...`) — aceitável apenas em ambiente local/H2.
- **Prod** (`application-prod.yml`): `security.jwt.secret: ${JWT_SECRET}` **sem fallback** — a
  aplicação falha ao subir (`Could not resolve placeholder`) se a variável não existir. **Nunca
  adicione um fallback em produção.**
- `JWT_SECRET` e senhas de usuário **nunca** devem ser commitadas no repositório, nem em
  `.env` (apenas `.env.example` com placeholders), nem em `schema.sql`, nem em logs.

## Usuários

- **Dev**: `DevUserSeeder` (`@Profile("!prod")`) cria `admin/admin123`, `operator/operator123`,
  `viewer/viewer123` automaticamente no H2 em memória. **Nunca ative esse seeder em produção.**
- **Prod**: usuários são criados manualmente na tabela `users` do RDS (BCrypt via
  `pgcrypto.crypt()`/`gen_salt('bf')` ou pela própria aplicação). Credenciais de produção nunca
  devem ser as mesmas de dev — são bancos de dados completamente separados (H2 local vs. RDS).

## Ao adicionar um novo endpoint protegido

1. Decida o nível mínimo de role necessário (ADMIN / OPERATOR / VIEWER / qualquer autenticado).
2. Adicione a regra explícita em `SecurityConfig` — não confie no catch-all.
3. Documente o nível de acesso no README (tabela de endpoints já tem coluna de role mínima).
4. Escreva teste cobrindo o caso de acesso negado (403) e o caso de acesso permitido.
