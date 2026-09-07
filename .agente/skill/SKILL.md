# SKILL — Como atuar neste projeto (MCC Credit Assignment API)

Este documento define o **fluxo de trabalho obrigatório** para qualquer agente de IA que for
implementar, corrigir ou revisar código neste repositório.

## 1. Antes de codificar — sempre faça isso primeiro

1. **Leia `knowledge/architecture.md`** para entender em qual camada sua mudança se encaixa.
2. **Leia `knowledge/domain-rules.md`** se a tarefa envolver regras de negócio (precificação,
   liquidação, câmbio, cedentes/recebíveis).
3. **Leia `knowledge/security.md`** se a tarefa envolver autenticação, autorização ou novos
   endpoints REST.
4. **Procure por código existente reaproveitável** antes de criar algo novo (ver seção 3).
5. Se a tarefa for ambígua ou conflitar com algo documentado aqui, **pergunte ao usuário antes de
   prosseguir** — não assuma.

## 2. Fluxo de implementação

1. Explore o código existente relacionado (grep/glob) antes de criar arquivos novos.
2. Implemente seguindo os padrões já estabelecidos na camada correspondente (ver
   `knowledge/architecture.md` para convenções exatas de nomenclatura e localização).
3. **Escreva e/ou atualize os testes unitários** cobrindo a mudança (ver `knowledge/testing.md`).
4. Rode o build e os testes localmente antes de considerar a tarefa concluída:
   ```
   mvn compile
   mvn test
   ```
   (Maven não está no PATH neste ambiente — use o `mvn.cmd` embutido no IntelliJ, ou o `mvnw.cmd`
   do próprio projeto.)
5. Se a mudança envolver schema de banco, atualize `src/main/resources/db/schema.sql` e sincronize
   com o modelo de entidades JPA (ver `knowledge/database.md`).
6. Se a mudança envolver endpoints REST, atualize a documentação relevante (README, springdoc
   annotations `@Operation`/`@Tag`) e, se for uma mudança de contrato relevante para o
   front-end, gere/atualize documentação dedicada como já foi feito em `docs/AUTHENTICATION.md`.

## 3. Reaproveitamento de código — checklist obrigatório

Antes de criar qualquer classe, método, DTO, exception ou configuração nova, verifique:

- [ ] Já existe uma classe/interface na mesma camada que resolve (ou quase resolve) o problema?
- [ ] Existe um *port* (`domain/port/in` ou `domain/port/out`) que já expõe o comportamento
      necessário?
- [ ] Existe uma *exception* de domínio (`domain/exception/*`) equivalente à que você
      criaria?
- [ ] Existe um DTO em `application/dto/request` ou `application/dto/response` que já cobre o
      payload necessário, ou que pode ser estendido em vez de duplicado?
- [ ] Existe um *enum* de domínio (`domain/enums/*`) que já modela o valor que você precisa?
- [ ] A lógica que você está prestes a escrever já existe em outro *use case*/`ApplicationService`
      e poderia ser extraída para um método compartilhado em vez de duplicada?

Se a resposta a qualquer uma dessas perguntas for "sim", **reaproveite/estenda o código
existente** em vez de criar uma versão nova e redundante.

## 4. Proibições (ver `forbidden-actions.md` para a lista completa)

As três regras inegociáveis, sempre válidas independentemente do escopo da tarefa:

1. **Nunca pule os testes unitários.** Toda mudança de comportamento precisa de teste que a
   cubra, e a suíte completa deve passar (`mvn test`) antes de considerar o trabalho concluído.
2. **Nunca fuja da arquitetura hexagonal do projeto.** Domínio não depende de infraestrutura;
   controllers não acessam repositórios JPA diretamente (exceto o caso documentado do
   `ReportController`, que é uma exceção arquitetural intencional — ver `architecture.md`).
3. **Sempre verifique reaproveitamento de código antes de criar algo novo** (checklist da seção 3).

Consulte `knowledge/forbidden-actions.md` para a lista completa de guardrails de segurança,
infraestrutura e dados sensíveis.

## 5. Ao terminar uma tarefa

- Rode `mvn test` e confirme que todos os testes passam.
- Se a tarefa envolveu produção (RDS, Docker Hub, variáveis de ambiente sensíveis), **nunca**
  grave segredos em arquivos versionados — nem em `.env`, nem em código, nem em `schema.sql`.
- Resuma para o usuário exatamente o que foi alterado, testado e validado — sem exagerar o que
  foi verificado de fato.
