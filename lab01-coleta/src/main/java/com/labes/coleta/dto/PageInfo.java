package com.labes.coleta.dto;

/** Campo "pageInfo" retornado pela API GraphQL, usado para paginação. */
public record PageInfo(boolean hasNextPage, String endCursor) {
}
