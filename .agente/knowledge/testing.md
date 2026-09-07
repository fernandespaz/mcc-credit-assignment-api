# Testes — Convenções e obrigatoriedade

## Regra inegociável

**Nenhuma tarefa está concluída sem rodar `mvn test` (ou `mvnw.cmd test`) e confirmar que a suíte
inteira passa.** Isso vale mesmo para mudanças que "parecem" triviais (configuração, README,
schema) — se houver qualquer chance de a mudança quebrar o build ou o contexto Spring, rode os
testes.

```bash
.\mvnw.cmd clean test
```

## Estado atual da cobertura (seja honesto sobre isso)

No momento, a suíte de testes automatizados é **mínima** — há apenas um teste de contexto
(`CreditAssignmentApplicationTests.contextLoads`). Isso **não é uma licença para não escrever
testes** ao adicionar funcionalidade nova — é uma dívida técnica existente que deve ser reduzida,
não aumentada:

- Ao adicionar uma regra de negócio nova (ex: nova `PricingStrategy`, nova validação de domínio),
  **escreva um teste unitário** para o cálculo/regra, mesmo que a suíte geral ainda seja pequena.
- Ao adicionar um endpoint novo com regras de autorização, **escreva um teste** cobrindo pelo
  menos o caso de sucesso e o caso de acesso negado (401/403).
- Ao corrigir um bug, **adicione um teste de regressão** que teria pego o bug antes do fix.

## O que validar manualmente quando não houver teste automatizado equivalente

Quando a área ainda não tiver testes automatizados (ex: fluxo de autenticação completo foi
validado manualmente via PowerShell/`Invoke-RestMethod` nesta sessão), documente no relatório
final para o usuário **exatamente** o que foi testado manualmente e como — não afirme "testado"
de forma vaga. Prefira sempre automatizar em vez de depender de validação manual repetida.

## Build

- `mvn compile` — compila.
- `mvn test` — roda os testes.
- `mvn clean test` — build limpo + testes (use antes de considerar qualquer tarefa "pronta").
- Maven não está no PATH neste ambiente Windows — use `.\mvnw.cmd` (wrapper do projeto) ou o
  `mvn.cmd` embutido no IntelliJ como alternativa.
