package com.labes.coleta.model;

/**
 * Tamanho médio (em linhas alteradas) dos PRs aceitos amostrados num ano.
 * amostraPRs = quantos PRs entraram de fato na média (pode ser menor que
 * N × nº de repositórios, se algum repositório tiver menos PRs aceitos que N no ano).
 */
public record TendenciaAnualTamanhoPR(int ano, double tamanhoMedioLinhas, int amostraPRs) {
}
