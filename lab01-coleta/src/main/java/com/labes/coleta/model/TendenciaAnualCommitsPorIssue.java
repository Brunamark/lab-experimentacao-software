package com.labes.coleta.model;

/**
 * Esforço médio, em commits, para fechar uma issue num ano (RQB03).
 *
 * <p>A média cobre apenas issues fechadas com Pull Request vinculado — é a população sobre a
 * qual a pergunta faz sentido ("issues fechadas com commits atrelados"). As duas contagens de
 * universo não são amostradas: vêm da contagem total da API e existem para que o leitor saiba
 * que fatia das issues fechadas a média representa, e se essa fatia mudou entre os anos.
 *
 * @param commitsMediosPorIssue     soma dos commits dos PRs que fecharam as issues amostradas,
 *                                  dividida por issuesAnalisadas
 * @param commitsMedianosPorIssue   mediana da mesma amostra. A distribuição de commits por PR
 *                                  tem cauda longa, e no piloto de 50 repositórios uma única
 *                                  issue deslocou a média de 2025 em 2×; sem a mediana ao lado
 *                                  não dá para saber se um pico anual é comportamento ou um
 *                                  caso extremo
 * @param issuesAnalisadas          quantas issues entraram de fato nas estatísticas
 * @param issuesComPrNoUniverso     total de issues fechadas com PR vinculado no ano
 * @param issuesFechadasNoUniverso  total de issues fechadas no ano, com ou sem PR
 */
public record TendenciaAnualCommitsPorIssue(
        int ano,
        double commitsMediosPorIssue,
        double commitsMedianosPorIssue,
        int issuesAnalisadas,
        long issuesComPrNoUniverso,
        long issuesFechadasNoUniverso
) {
}
