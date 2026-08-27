# Relatório — Laboratório de Experimentação de Software

| Campo | Valor |
|---|---|
| **Curso** | Engenharia de Software |
| **Disciplina** | Laboratório de Experimentação de Software |
| **Turno / Período** | Noite / 6º |
| **Professor(a)** | Danilo Maia |
| **Laboratório** | Lab01 — Características de Repositórios Populares + Setup do Kanban |
| **Grupo (trio)** | [Integrante 1] · [Integrante 2] · [Integrante 3] |
| **Link do repositório / GitHub Projects** | [preencher — obrigatório] |
| **Data de entrega** | [preencher] |

---

## 1. Introdução

Sistemas open-source populares concentram grande parte da atenção da comunidade de engenharia de software, mas as características que de fato os tornam populares — maturidade, ritmo de contribuição externa, frequência de lançamentos, atualização e escolha de linguagem — nem sempre são evidentes sem uma análise sistemática. Este laboratório investiga essas características a partir da mineração dos 1.000 repositórios com maior número de estrelas no GitHub, respondendo às sete Questões de Pesquisa (RQ01–RQ07) definidas no enunciado.

As Questões de Pesquisa do enunciado são:

- **RQ01.** Sistemas populares são maduros/antigos? (idade do repositório)
- **RQ02.** Sistemas populares recebem muita contribuição externa? (total de pull requests aceitas)
- **RQ03.** Sistemas populares lançam releases com frequência? (total de releases)
- **RQ04.** Sistemas populares são atualizados com frequência? (tempo até a última atualização)
- **RQ05.** Sistemas populares são escritos nas linguagens mais populares? (linguagem primária)
- **RQ06.** Sistemas populares possuem um alto percentual de issues fechadas? (razão issues fechadas/total)
- **RQ07.** Sistemas em linguagens mais populares recebem mais contribuição, lançam mais releases e são atualizados com mais frequência? (RQ02, RQ03 e RQ04 por linguagem)

Hipóteses informais do grupo, antes da análise dos dados:

> *[hipótese do grupo para cada RQ01–RQ07 — preencher antes da coleta]*

Além do enunciado, o grupo propôs duas Questões de Pesquisa de Validação (RQV1 e RQV2), voltadas não às características dos repositórios em si, mas à confiabilidade do próprio pipeline de coleta de dados construído para responder às RQ01–RQ07:

- **RQV1** — A extração via API GraphQL do GitHub cobre a totalidade dos itens esperados, sem lacunas de paginação, e produz resultados idênticos em execuções repetidas nas mesmas condições?
- **RQV2** — A extração via API GraphQL do GitHub evita chamadas HTTP redundantes e recupera-se corretamente de falhas transitórias (5xx) via retry, sem contabilizar a tentativa de recuperação como uma chamada redundante?

O detalhamento da metodologia usada para validar RQV1 e RQV2 está na Seção 3.6.

---

## 2. Contexto

Este relatório documenta o Lab01 da disciplina, primeiro laboratório do semestre, que também dá início ao uso do GitHub Projects (v2) como quadro Kanban do grupo, mantido até o Lab05. O objeto de estudo são os 1.000 repositórios com maior número de estrelas no GitHub, minerados via API GraphQL complementada por chamadas REST.

Como referência para a definição de "linguagens mais populares" usada na RQ05, o grupo adota:

> *[indicar a fonte escolhida — TIOBE Index, GitHut ou GitHub Octoverse — e mantê-la ao longo de todo o relatório]*

Como base metodológica para a validação da confiabilidade do pipeline de coleta (RQV1 e RQV2), o grupo utilizou cenários de teste de comportamento (BDD) escritos em Cucumber/Gherkin, executados contra a API real do GitHub sob o perfil `realtest`, tratando o próprio processo de extração de dados como objeto de verificação, e não apenas os dados extraídos.

---

## 3. Metodologia

### 3.1 Principais Desafios

- Limite de taxa (rate limit) da API do GitHub ao consultar centenas/milhares de repositórios.
- Paginação de grandes volumes de dados, exigindo verificação explícita de que o laço de coleta respeita o indicador `hasNextPage` sem gerar chamadas adicionais após seu término.
- Falhas transitórias (respostas 5xx) não são reproduzíveis de forma determinística contra a API real: não é possível garantir que uma página específica falhará com `502` na primeira tentativa, o que limitou a validação empírica do mecanismo de retry a um cenário de execução sem falhas.
- Mistura de registros no `CallLog`: como o cenário de idempotência compara duas execuções da mesma coleta usando o mesmo registro de chamadas, a métrica de redundância passa a misturar a repetição esperada da segunda execução com possíveis duplicidades internas, exigindo leitura cuidadosa por execução lógica.
- Divergências observadas em execuções anteriores entre o XML do Cucumber e o resumo do Surefire, exigindo que os artefatos de relatório sejam sempre coletados a partir da mesma execução antes de qualquer comparação.
- Ausência de histórico de mudança de status consultável via API no GitHub Projects, exigindo snapshots manuais recorrentes a cada sprint.

### 3.2 Tomadas de Decisão

- O cenário original que induzia uma resposta `502` determinística foi substituído por um cenário de execução real sem falhas, já que a API real do GitHub não permite garantir a ocorrência controlada de um erro 502 na primeira tentativa — a recuperação determinística desse caso não é apresentada como evidência validada nesta execução.
- As métricas de validação do pipeline (`total_chamadas`, `chamadas_unicas`, `chamadas_redundantes`, `tentativas_de_retry`, `taxa_redundancia`, `cobertura_paginacao_pct`, `taxa_idempotencia_pct`) foram definidas e registradas em formato longo por cenário no arquivo `metricas.csv`, permitindo comparação cenário a cenário.
- A execução dos testes reais foi isolada por meio de tags Cucumber (`@real` e `not @mock`) e do perfil Spring `realtest`, com autenticação via `GITHUB_TOKEN` carregado de um arquivo `.env`, nunca reproduzido no relatório.
- Limite de WIP definido para a coluna Doing:

> *[número escolhido e justificativa]*

### 3.3 Etapas

O processo seguiu a estrutura de sprints definida no enunciado (Lab01S01 a S03 + Relatório Final). A validação automatizada do pipeline de coleta (RQV1/RQV2) foi tratada como parte do trabalho de S03, antes da consolidação da análise das RQ01–RQ07.

| Sprint | Entregas | Responsável(is) | Issues (nº) |
|---|---|---|---|
| Lab01S01 | Consulta GraphQL para 100 repositórios; requisição automática; GitHub Projects criado com colunas e limite de WIP definidos. | [preencher] | [preencher] |
| Lab01S02 | Paginação para 1000 repositórios; dados exportados em .csv; primeira versão do relatório com hipóteses informais; board atualizado com primeiro snapshot. | [preencher] | [preencher] |
| Lab01S03 | Análise e visualização de dados para as sete RQs; validação automatizada do pipeline de coleta (cenários RQV1/RQV2) como inovação metodológica. | [preencher] | [preencher] |
| Relatório Final | Elaboração do documento final, incluindo print do board e política de WIP em uso. | [preencher] | [preencher] |

**Configuração do processo**

Colunas do board (campo Status): `Backlog → To Do → Doing → Review → Done`.

Política de limite de WIP para a coluna Doing:

> *[descrever a política em uso]*

> *[inserir aqui o print do quadro Kanban (GitHub Projects) ao final do laboratório]*

### 3.4 Ferramentas

- Java 21 e Maven, para o script de coleta e a suíte de testes.
- Spring Boot, como framework de aplicação e gestão de perfis de execução (perfil `realtest`).
- Cucumber, para os cenários de comportamento (BDD) que validam paginação, idempotência, ausência de chamadas redundantes e comportamento de retry.
- API GraphQL do GitHub, como fonte primária de coleta, complementada por chamadas REST — consulta e consumo implementados em script próprio do grupo, sem bibliotecas de terceiros para acesso à API.
- GitHub Projects (v2), como ferramenta de processo, com board vinculado ao repositório do grupo: *[link do repositório / GitHub Projects]*

> *[demais ferramentas de análise/visualização de dados usadas para RQ01–RQ07 — ex.: Python/Pandas, Matplotlib/Seaborn]*

### 3.5 Tabela de Métricas

A tabela abaixo relaciona cada Questão de Pesquisa do enunciado à sua métrica, definição operacional e ferramenta de coleta.

| RQ | Métrica | Definição Operacional | Unidade | Ferramenta / Fonte |
|---|---|---|---|---|
| RQ01 | Idade do repositório | Data atual − data de criação do repositório | Dias | Script GraphQL (API do GitHub) |
| RQ02 | Total de pull requests aceitas | Contagem de PRs com estado "merged" no repositório | Nº de PRs | Script GraphQL (API do GitHub) |
| RQ03 | Total de releases | Contagem de releases publicadas no repositório | Nº de releases | Script GraphQL (API do GitHub) |
| RQ04 | Tempo até a última atualização | Data atual − data do último commit/push | Dias | Script GraphQL (API do GitHub) |
| RQ05 | Linguagem primária de cada repositório | Linguagem principal reportada pela API, comparada à lista de linguagens mais populares (fonte: *[preencher — TIOBE/GitHut/Octoverse]*) | Categórica | Script GraphQL (API do GitHub) + [fonte externa] |
| RQ06 | Razão entre issues fechadas e total de issues | issues_fechadas / issues_totais | Percentual | Script GraphQL (API do GitHub) |
| RQ07 | RQ02, RQ03 e RQ04 segmentadas por linguagem | Mediana de cada métrica agrupada por linguagem primária | Conforme métrica de origem | Análise derivada (Pandas/planilha) |

A tabela seguinte apresenta as métricas adicionais definidas pelo grupo para validar a confiabilidade do pipeline de coleta (RQV1 e RQV2), detalhadas na Seção 3.6.

| RQ | Métrica | Definição Operacional | Unidade | Ferramenta / Fonte |
|---|---|---|---|---|
| RQV1 | `cobertura_paginacao_pct` | `itens_coletados / itens_esperados × 100` | % | CallLog / ColetaSteps.java (Cucumber) |
| RQV1 | `taxa_idempotencia_pct` | Percentual de itens iguais, por posição, entre duas execuções | % | CallLog / ColetaSteps.java (Cucumber) |
| RQV2 | `chamadas_redundantes` | Requisições que repetem uma assinatura HTTP já registrada | Nº de chamadas | CallLog.java |
| RQV2 | `taxa_redundancia` | `chamadas_redundantes / total_chamadas` | Proporção | CallLog.java |
| RQV2 | `tentativas_de_retry` | Repetições associadas a uma chamada que falhou (5xx) | Nº de tentativas | GitHubGraphQLClient.java |

### 3.6 Inovações Propostas pelo Grupo (30% da nota)

Como contribuição além do enunciado, o grupo tratou o próprio pipeline de coleta de dados como objeto de verificação, e não apenas como um meio de obter os dados das RQ01–RQ07. Foram propostas duas Questões de Pesquisa de Validação (RQV1 e RQV2), avaliadas por meio de cenários automatizados de teste de comportamento (Cucumber) executados contra a API real do GitHub, com o perfil `realtest` e o comando:

```bash
cd lab01-coleta
set -a
source .env
set +a

mvn clean \
  -Dspring.profiles.active=realtest \
  -Dcucumber.filter.tags="@real and not @mock" \
  test
```

O relatório Surefire dessa execução registrou oito cenários analisados, cinco executados, três ignorados (pertencentes à funcionalidade de fallback, não interpretados como resultados desta execução), nenhuma falha e nenhum erro.

**RQV1** está relacionada à completude e à reprodutibilidade da coleta, e foi avaliada por três cenários: coleta de 100 repositórios com páginas de 10; encerramento natural quando `hasNextPage` é falso; e duas execuções da mesma coleta com os mesmos parâmetros.

**RQV2** está relacionada ao uso eficiente da API e à resiliência da coleta diante de falhas transitórias, e foi avaliada por dois cenários reais: coleta sem falhas e sem chamadas redundantes; e coleta real sem falhas e sem tentativas de retry desnecessárias.

A instrumentação dessas métricas está implementada em `ColetaSteps.java`, `Hooks.java` e `CallLog.java` (asserções) e em `GitHubGraphQLClient.java` (tratamento de retry para respostas 5xx). Os resultados obtidos e sua discussão aparecem nas Seções 4.1, 4.3 e 5.

---

## 4. Resultados

### 4.1 Coleta de Dados

> *[volume final de dados coletado e analisado para RQ01–RQ07: quantos dos 1.000 repositórios buscados tinham dados completos, período coberto, outliers e dados ausentes identificados]*

Como evidência de que o pipeline estava apto a ser escalado para os 1.000 repositórios, a validação automatizada (RQV1/RQV2) registrou os seguintes resultados nos cenários de paginação e idempotência:

**Paginação (por cenário)**

| Métrica | Valor por cenário |
|---|---:|
| `total_chamadas` | 15 |
| `chamadas_unicas` | 15 |
| `chamadas_redundantes` | 0 |
| `tentativas_de_retry` | 0 |
| `taxa_redundancia` | 0,0 |

**Idempotência (duas execuções)**

| Métrica | Valor |
|---|---:|
| `taxa_idempotencia_pct` | 100,0% |
| `total_chamadas` | 4 |
| `chamadas_unicas` | 2 |
| `chamadas_redundantes` | 2 |
| `tentativas_de_retry` | 0 |
| `taxa_redundancia` | 0,5 |

A igualdade entre `total_chamadas` e `chamadas_unicas` nos cenários de paginação indica ausência de repetição registrada nas chamadas observadas, e o teste confirmou que nenhuma requisição adicional ocorreu após `hasNextPage = false`. As duas chamadas redundantes do cenário de idempotência correspondem à segunda execução da mesma coleta compartilhando o `CallLog`, não a duplicidade interna da paginação.

Para os cenários reais de coleta sem falhas, o CSV registrou zero chamadas redundantes, taxa de redundância igual a zero e `tentativas_de_retry = 0`, valor compatível com uma execução em que a API respondeu normalmente — o que demonstra ausência de necessidade de recuperação, mas não comprova o funcionamento do retry após uma resposta `502`.

### 4.2 Visualização Gráfica

> *[inserir aqui os gráficos do grupo para RQ01–RQ07, um por RQ, cada um precedido da pergunta que ele responde]*

Para RQV1 e RQV2, os resultados são binários/percentuais por cenário (aprovado/reprovado, percentuais de idempotência e redundância) e foram reportados diretamente nas tabelas da Seção 4.1, sem necessidade de visualização gráfica adicional.

### 4.3 Discussão

> *[para cada RQ01–RQ07: comparar a hipótese informal da Introdução com o resultado obtido (confirmada, refutada ou parcialmente confirmada) e por quê]*

**RQV1 — completude e reprodutibilidade**

Os testes apoiam parcialmente a RQV1: a paginação percorreu as páginas observadas sem repetir chamadas, a coleta encerrou corretamente após a indicação de ausência de próxima página, e duas execuções produziram resultados idênticos, com idempotência de 100%. A hipótese de completude, no entanto, não pode ser considerada totalmente comprovada apenas com esses artefatos, pois o cenário de paginação executado verifica somente que pelo menos um repositório foi coletado — não há comparação explícita entre `itens_coletados` e `itens_esperados`, e a métrica `cobertura_paginacao_pct` não apareceu no CSV desta execução.

**RQV2 — eficiência e resiliência**

Os resultados também apoiam parcialmente a RQV2: não foram observadas chamadas redundantes nas coletas normais, não houve retry desnecessário quando a API não falhou, e a coleta real terminou com sucesso. A propriedade mais específica da hipótese — que uma resposta `502` é seguida por exatamente um retry bem-sucedido — não foi validada nesta execução, pois essa condição exige uma falha reproduzível que não pode ser garantida usando somente a API real do GitHub. A ausência de `502` nos testes reais não deve ser interpretada como prova de que o mecanismo de retry foi de fato exercitado.

**Ameaças à validade específicas da inovação (RQV1/RQV2):**

1. Falha transitória não determinística: a API real não permite induzir um `502` na primeira tentativa de uma página; o teste real valida o comportamento normal, não a recuperação controlada.
2. Cobertura de paginação incompleta: o cenário usa a asserção "ao menos 1 repositório", em vez de exigir exatamente 100 itens.
3. Métrica de cobertura ausente: existe um step para calcular `cobertura_paginacao_pct`, mas essa métrica não aparece no CSV atual.
4. Idempotência por posição: a comparação usa o menor tamanho entre as listas; diferenças de tamanho podem não ser totalmente refletidas.
5. Mistura no `CallLog` entre as duas execuções do cenário de idempotência, gerando chamadas redundantes esperadas na segunda execução.
6. CSV em modo append: execuções sucessivas podem misturar resultados de diferentes rodadas se o arquivo não for limpo antes da coleta.
7. Escopo das chamadas: `total_chamadas` inclui chamadas GraphQL e REST; avaliar exclusivamente a paginação GraphQL exige filtrar pelo endpoint `/graphql`.
8. Variação da API real: estrelas, datas e releases podem mudar entre execuções — a idempotência observada vale para as condições registradas, não como garantia permanente do estado do GitHub.

As inovações do grupo (Seção 3.6) acrescentam, ao restante do relatório, uma camada de confiança metodológica: antes de interpretar os resultados de RQ01–RQ07 como reflexo fiel do estado do GitHub, o grupo verificou — com evidência automatizada, embora parcial — que o próprio processo de coleta não perde itens por paginação incompleta nem infla artificialmente o uso da API com chamadas redundantes.

---

## 5. Conclusão

> *[síntese das respostas às RQ01–RQ07, sem repetir números já discutidos em detalhe; limitações do estudo (tamanho de amostra, período de coleta)]*

Quanto à validação do pipeline de coleta: a execução real forneceu evidências favoráveis de que o pipeline percorre as páginas observadas sem repetir chamadas, respeita o encerramento indicado por `hasNextPage = false`, produz resultados idênticos em duas execuções nas mesmas condições observadas, e não registra chamadas redundantes nem retries em uma coleta normal sem falhas.

A RQV1 foi atendida parcialmente, com evidência de paginação sem repetição e idempotência de 100%, mas ainda sem comprovação explícita de cobertura percentual de 100% dos itens esperados. A RQV2 também foi atendida parcialmente: a ausência de chamadas redundantes foi observada e o comportamento normal foi bem-sucedido, porém a recuperação específica de uma falha `502` não foi exercitada pela API real e, portanto, não pode ser declarada validada com base nesta execução.

Com mais tempo, o grupo expandiria essa inovação simulando a API do GitHub (mock de respostas 502) para validar deterministicamente o mecanismo de retry, e adicionaria a asserção explícita de `cobertura_paginacao_pct = 100%` ao cenário de paginação, hoje limitado à verificação de "ao menos 1 repositório".

---

## 5. Referências

- ZUSE, Horst. A framework of software measurement. Walter de Gruyter, 2013.
- *[demais referências usadas — ex.: documentação da API GraphQL do GitHub, documentação do Cucumber, fonte de linguagens mais populares para RQ05]*

Vídeo de referência (conforme orientação do template): https://www.youtube.com/shorts/YwnaeO95AN8
