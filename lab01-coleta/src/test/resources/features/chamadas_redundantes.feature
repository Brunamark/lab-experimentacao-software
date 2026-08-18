# language: pt
Funcionalidade: Chamadas redundantes à API
  Como responsável pela coleta de dados do GitHub
  Quero identificar chamadas repetidas que não deveriam acontecer
  Para não estourar o rate limit nem inflar as métricas de uso da API

  Cenário: Coleta sem falhas não deve repetir nenhuma requisição
    Dado que a API do GitHub tem 4 repositórios disponíveis para a busca
    E o tamanho de página configurado é 2
    Quando a coleta é executada
    Então nenhuma chamada redundante deve ser registrada

  Cenário: Retry após falha transitória não deve ser contado como redundância
    Dado que a API do GitHub tem 2 repositórios disponíveis para a busca
    E o tamanho de página configurado é 2
    E a primeira tentativa da página vai falhar com erro 502
    Quando a coleta é executada
    Então a coleta deve ter sucesso mesmo assim
    E deve haver exatamente 1 tentativa de retry
    E nenhuma chamada redundante deve ser registrada
