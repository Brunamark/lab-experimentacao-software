package com.labes.coleta.dto;

import java.util.List;

/** Campo "search" retornado pela query GraphQL. */
public record SearchResult(
        int repositoryCount,
        PageInfo pageInfo,
        List<RepositoryNode> nodes
) {
}
