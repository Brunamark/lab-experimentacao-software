package com.labes.coleta.model;

import java.time.Instant;

/**
 * Dado coletado de um repositório, pronto para virar uma linha do CSV.
 *
 * @param ultimoPush           data do último push; null em repositório sem commits
 * @param diasDesdeUltimoPush  -1 quando não há data de push (sentinela de dado ausente)
 */
public record RepositorioMetrica(
        String nome,
        long estrelas,
        long idadeEmMeses,
        int totalReleases,
        Instant ultimoPush,
        Instant ultimaAtualizacao,
        long diasDesdeUltimoPush
) {

    /** Valor de {@code diasDesdeUltimoPush} quando o repositório não tem data de push. */
    public static final long SEM_DATA_DE_PUSH = -1;

    /** Descarta da análise os repositórios sem data de push. */
    public boolean temDataDePush() {
        return diasDesdeUltimoPush != SEM_DATA_DE_PUSH;
    }

    /**
     * Cópia com o total de releases corrigido — o GraphQL satura em 1000 e o valor real
     * vem da API REST.
     *
     * <p>Centralizar aqui a chamada do construtor evita que cada ponto de cópia quebre
     * quando formos acrescentar um campo novo ao record.
     */
    public RepositorioMetrica comTotalReleases(int novoTotal) {
        return new RepositorioMetrica(nome, estrelas, idadeEmMeses, novoTotal,
                ultimoPush, ultimaAtualizacao, diasDesdeUltimoPush);
    }
}