package com.labes.coleta.model;

/**
 * Dado coletado de um repositório, pronto para virar uma linha do CSV.
 * Nesta issue (estrutura inicial), só nome e estrelas.
 */
public record RepositorioMetrica(
        String nome,
        long estrelas
) {
}