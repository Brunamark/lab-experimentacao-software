# language: pt
Funcionalidade: Cobertura de paginação
  Como responsável pela coleta de dados do GitHub
  Quero garantir que a paginação percorre páginas corretamente
  Para que a coleta funcione com API real

  @real
  Cenário: Coleta executa paginação na API real
    Dado o total de repositórios para coleta é 100
    E o tamanho de página configurado é 10
    Quando a coleta é executada
    Então devem ser coletados ao menos 1 repositórios

  @real
  Cenário: Coleta encerra naturalmente quando não há próxima página
    Dado o total de repositórios para coleta é 100
    E o tamanho de página configurado é 10
    Quando a coleta é executada
    Então nenhuma requisição deve ser feita após "hasNextPage" ser falso
