# language: pt
Funcionalidade: Chamadas redundantes à API
  Como responsável pela coleta de dados do GitHub
  Quero identificar chamadas repetidas que não deveriam acontecer
  Para não estourar o rate limit nem inflar as métricas de uso da API

  @real
  Cenário: Coleta sem falhas não deve repetir nenhuma requisição
    Dado que a API do GitHub tem 4 repositórios disponíveis para a busca
    E o tamanho de página configurado é 2
    Quando a coleta é executada
    Então nenhuma chamada redundante deve ser registrada

  @real
  Cenário: Coleta real sem falhas não registra tentativas de retry
    Dado que a API do GitHub tem 4 repositórios disponíveis para a busca
    E o tamanho de página configurado é 2
    Quando a coleta é executada
    Então a coleta deve ter sucesso mesmo assim
    E deve haver exatamente 0 tentativa de retry
    E nenhuma chamada redundante deve ser registrada
