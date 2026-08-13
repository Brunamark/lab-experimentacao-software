package com.labes.coleta.dto;

import java.time.Instant;

/**
 * Representa um nó "Repository" dentro de search.nodes.
 */
public record RepositoryNode(
        String nameWithOwner,
        int stargazerCount,
        Instant createdAt,
        Instant updatedAt,
        Instant pushedAt
) {
}