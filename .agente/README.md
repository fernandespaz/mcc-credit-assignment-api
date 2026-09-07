# `.agente/` — Contexto para Agentes de IA

Esta pasta contém as instruções, regras e base de conhecimento que qualquer agente de IA
(GitHub Copilot, Claude, ChatGPT, ou qualquer outro assistente autônomo) **deve ler antes de
executar qualquer tarefa** neste repositório.

## Estrutura

```
.agente/
├── README.md              ← você está aqui
├── skill/
│   └── SKILL.md            ← como o agente deve atuar: fluxo de trabalho, proibições, checklist
└── knowledge/
    ├── architecture.md      ← arquitetura hexagonal, camadas, convenções de pacotes
    ├── domain-rules.md       ← regras de negócio do domínio de cessão de crédito
    ├── security.md          ← autenticação JWT, roles, regras de autorização
    ├── database.md          ← modelo de dados, RDS IAM auth, migrações
    ├── testing.md           ← convenções e obrigatoriedade de testes
    └── forbidden-actions.md ← lista explícita de ações proibidas (guardrails)
```

## Regra de ouro

> **Antes de escrever qualquer código, o agente deve ler `skill/SKILL.md` e os arquivos
> relevantes em `knowledge/`.** Se uma tarefa conflitar com algo documentado aqui, o agente deve
> parar e perguntar ao usuário antes de prosseguir — nunca decidir sozinho quebrar uma regra
> listada em `forbidden-actions.md`.

## Manutenção

Sempre que uma decisão arquitetural importante for tomada, ou uma nova convenção for adotada,
atualize o arquivo correspondente em `knowledge/`. Esta pasta é código vivo — desatualizada, ela
é pior do que não existir, porque passa a gerar decisões incorretas.
