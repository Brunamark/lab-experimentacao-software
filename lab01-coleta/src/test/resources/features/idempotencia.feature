# language: pt
Funcionalidade: Idempotência da requisição
  Como responsável pela coleta de dados do GitHub
  Quero que repetir a mesma coleta produza sempre o mesmo resultado
  Para confiar nos números usados na análise

  @real
  Cenário: Duas execuções da mesma coleta geram o mesmo conjunto de dados
    Dado que a API do GitHub tem 4 repositórios disponíveis para a busca
    E o tamanho de página configurado é 2
    Quando a coleta é executada duas vezes seguidas com os mesmos parâmetros
    Então os dois resultados devem ser idênticos
    E a taxa de idempotência deve ser de 100%
