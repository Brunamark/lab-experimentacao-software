package com.labes.coleta.model;

/** Contagem agregada (entre os repositórios amostrados) de PRs criados/aceitos num ano. */
public record TendenciaAnualPullRequests(
        int ano,
        long prsCriadas,
        long prsAceitas,
        double taxaAceitacao
) {
}
