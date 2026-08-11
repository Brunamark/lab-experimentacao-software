# Laboratório de Experimentação de Software

Repositório do grupo para os laboratórios da disciplina **Laboratório de Experimentação de Software** — Engenharia de Software (turno noite, 6º período).

**Professor:** Danilo Maia
**Grupo:** `<nome do grupo>`
**Integrantes:** `Bruna Costa Narkowisk`, `Thiago Branco`, `<Integrante C>`

---

## 📋 Sobre o Lab01

Este laboratório tem dois objetivos:

1. **Minerar dados** dos 1.000 repositórios mais populares (mais estrelas) do GitHub, via API GraphQL, para responder a questões de pesquisa sobre as características desses sistemas.
2. **Configurar o GitHub Projects (Kanban)** que acompanhará o grupo durante todo o semestre.

## 🔍 Questões de Pesquisa (RQs)

| RQ | Pergunta | Métrica |
|---|---|---|
| RQ01 | Sistemas populares são maduros/antigos? | Idade do repositório |
| RQ02 | Recebem muita contribuição externa? | Total de PRs aceitas |
| RQ03 | Lançam releases com frequência? | Total de releases |
| RQ04 | São atualizados com frequência? | Tempo até a última atualização |
| RQ05 | São escritos nas linguagens mais populares? | Linguagem primária (fonte: `<TIOBE / GitHut / Octoverse>`) |
| RQ06 | Têm alto percentual de issues fechadas? | Issues fechadas / total de issues |
| RQ07 (bônus) | Linguagens populares → mais contribuição, releases e atualizações? | RQ02, RQ03, RQ04 por linguagem |

## ⚙️ Como executar

### Pré-requisitos
- `< Node.js .>`
- Token de acesso pessoal do GitHub (variável de ambiente `GITHUB_TOKEN`)

### Instalação
```bash
git clone <link-do-repositorio>
cd <nome-do-repo>
<comando de instalação de dependências>
```

### Execução
```bash
<comando para rodar a consulta GraphQL>
```

> ⚠️ **Observação:** por exigência do laboratório, a consulta à API do GitHub é feita **sem bibliotecas de terceiros** — apenas requisições GraphQL escritas e consumidas por script próprio do grupo.

## 📁 Estrutura do repositório

```
.
├── src/            # scripts de coleta e análise
├── data/           # arquivos .csv gerados (100 e 1000 repositórios)
├── docs/           # relatório final e documentação
└── README.md
```

## 📊 Resultados

Os resultados detalhados (valores medianos, discussão hipótese vs. resultado) estão no **Relatório Final**: `<link para docs/relatorio-final.md ou .pdf>`.

## 🗂️ Processo de Desenvolvimento (Kanban)

O grupo utiliza o **GitHub Projects (v2)** para gestão das tarefas:

- **Board:** `<link do GitHub Projects>`
- **Colunas:** Backlog → To Do → Doing → Review → Done
- **Limite de WIP (Doing):** `<número>` — `<justificativa breve>`
- Cada commit referencia a Issue correspondente (ex.: `#12 implementa consulta GraphQL`)

### Sprints

| Sprint | Entrega | Status |
|---|---|---|
| Lab01S01 | Consulta GraphQL (100 repos) + Projects configurado | `<✅ / 🔄 / ⏳>` |
| Lab01S02 | Paginação (1000 repos) + CSV + 1ª versão do relatório | `<✅ / 🔄 / ⏳>` |
| Lab01S03 | Análise e visualização das RQs | `<✅ / 🔄 / ⏳>` |
| Relatório Final | Documento consolidado | `<✅ / 🔄 / ⏳>` |

## 👥 Divisão de tarefas

| Integrante | Responsabilidade |
|---|---|
| `<A>` | RQ01, RQ02 |
| `<B>` | RQ03, RQ04 |
| `<C>` | RQ05, RQ06, RQ07 (bônus) |

## 📄 Licença

`<opcional — ex.: MIT, ou "Uso acadêmico">`
