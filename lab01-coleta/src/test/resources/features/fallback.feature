# language: pt
@mock
Funcionalidade: Comportamento de fallback
  Como responsável pela coleta de dados do GitHub
  Quero que o fallback da API REST seja acionado apenas quando necessário
  E que ele não derrube a coleta quando falhar
  Para manter a métrica de releases confiável mesmo em cenários de erro

  Cenário: Fallback é acionado quando o GraphQL satura o total de releases em 1000
    Dado um repositório cujo total de releases no GraphQL vem saturado em 1000
    E a API REST informa que o total real de releases é 6850
    Quando a coleta é executada
    Então o total de releases do repositório deve ser 6850
    E o fallback via API REST deve ter sido acionado

  Cenário: Fallback não é acionado quando o total de releases está abaixo do teto
    Dado um repositório com 42 releases no GraphQL
    Quando a coleta é executada
    Então o total de releases do repositório deve ser 42
    E o fallback via API REST não deve ter sido acionado

  Cenário: Falha da API REST no fallback não derruba a coleta
    Dado um repositório cujo total de releases no GraphQL vem saturado em 1000
    E a API REST de releases está indisponível
    Quando a coleta é executada
    Então a coleta deve ter sucesso mesmo assim
    E o total de releases do repositório deve ser 1000
