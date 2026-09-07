# Regras de domínio — Cessão de Crédito (Credit Assignment)

## Entidades principais

- **Assignor** (Cedente): pessoa/empresa que cede recebíveis. Pode ser desativado
  (`deactivate()`), nunca excluído fisicamente (soft-delete via status).
- **Receivable** (Recebível): título a receber (duplicata, cheque pré-datado etc.), vinculado a um
  `Assignor`. Possui `ReceivableType`, `faceValue`, `termMonths`, moedas de ativo/pagamento.
  Estados possíveis incluem liquidado/cancelado — transições são validadas no próprio domínio
  (ex: `receivable.markSettled()` lança exceção de domínio se já liquidado/cancelado).
- **Settlement** (Liquidação): resultado da liquidação de um `Receivable` — calcula valor
  presente (PV) usando a estratégia de precificação do tipo do recebível, e converte moeda se
  `isCrossCurrency()` for verdadeiro.
- **ExchangeRate** (Taxa de câmbio): par de moedas com taxa vigente, usado na conversão de PV em
  liquidações cross-currency.
- **User**: usuário da aplicação com uma `UserRole` (ADMIN/OPERATOR/VIEWER) — implementa
  `UserDetails` do Spring Security diretamente (ver `security.md`).

## Regra de precificação (Pricing Engine)

Fórmula: `PV = FV / (1 + baseRate + spread) ^ termMonths`

- Cada `ReceivableType` tem um **spread de risco diferente**, encapsulado em uma implementação de
  `PricingStrategy` (ver `architecture.md` → Strategy Pattern).
- **Nunca** hardcode o spread dentro de um `ApplicationService` — sempre através de
  `PricingContext.strategyFor(type)`.
- Ao adicionar um novo `ReceivableType`, é **obrigatório** criar a `PricingStrategy`
  correspondente e registrá-la no `PricingContext`, e cobrir com teste unitário o cálculo de PV
  esperado.

## Regra de concorrência na liquidação (crítica — não enfraquecer)

`SettlementApplicationService.execute()` usa:
- `@Transactional(isolation = Isolation.SERIALIZABLE)`
- `receivableRepository.findByIdWithLock(id)` (lock pessimista `SELECT ... FOR UPDATE`)

Isso previne **double-settlement**: duas requisições simultâneas tentando liquidar o mesmo
recebível. Qualquer mudança nessa área precisa preservar essa garantia — se não tiver certeza do
impacto, pergunte ao usuário antes de alterar isolamento/lock.

## Conversão de moeda

- Liquidação só busca `ExchangeRate` se `receivable.isCrossCurrency()` for verdadeiro (moeda do
  ativo ≠ moeda de pagamento).
- Se a taxa não existir para o par de moedas, lança `ExchangeRateNotFoundException` — a
  liquidação **falha**, não segue com taxa 1:1 nem valor nulo silencioso.

## Regras de imutabilidade

- Entidades de domínio (`domain.entity.*`) devem se comportar como imutáveis sempre que possível:
  operações de mudança de estado (ex: `markSettled()`, `deactivate()`) retornam uma nova
  instância/estado validado, em vez de mutação silenciosa sem validação.
