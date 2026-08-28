package com.labes.coleta.dto;

import java.time.Instant;

/**
 * Representa um nó "Repository" dentro de search.nodes.
 *
 * <p>Todos os campos aninhados podem vir nulos: {@code primaryLanguage} é nulo em
 * repositórios que não são código (listas curadas, livros, roadmaps), e as conexões de
 * contagem podem faltar se o recurso estiver desabilitado no repositório.
 */
public record RepositoryNode(
        String nameWithOwner,
        int stargazerCount,
        Instant createdAt,
        Instant updatedAt,
        Instant pushedAt,
        Releases releases,
        // --- RQ05 ---
        PrimaryLanguage primaryLanguage,
        // --- RQ06 ---
        Contagem issuesAbertas,
        Contagem issuesFechadas,
        // --- RQ02 ---
        Contagem prsAceitas
) {

    /** Conexão "releases" — só a contagem total nos interessa. */
    public record Releases(int totalCount) {
    }

    /** Linguagem primária apontada pelo GitHub, escolhida por volume de bytes. */
    public record PrimaryLanguage(String name) {
    }

    /** Conexão da qual só a contagem interessa (issues e pull requests). */
    public record Contagem(int totalCount) {
    }
}
