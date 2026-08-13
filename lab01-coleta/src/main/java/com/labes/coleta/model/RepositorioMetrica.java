package com.labes.coleta.model;

/**
 * Dado coletado de um repositório, pronto para virar uma linha do CSV.
 */
public record RepositorioMetrica(
        String nome,
        long estrelas,
        long idadeEmMeses,
        long mesesDesdeUltimoPush
) {
}