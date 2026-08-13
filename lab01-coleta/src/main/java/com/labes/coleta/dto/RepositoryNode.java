package com.labes.coleta.dto;

/**
 * Representa um nó "Repository" dentro de search.nodes.
 * Nesta issue (estrutura inicial), traz apenas nome e nº de estrelas.
 */
public record RepositoryNode(
        String nameWithOwner,
        int stargazerCount
) {
}