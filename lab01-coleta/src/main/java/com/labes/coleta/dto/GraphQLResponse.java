package com.labes.coleta.dto;

import java.util.List;
import java.util.Map;

/** Envelope padrão de qualquer resposta da API GraphQL do GitHub: {"data": {...}, "errors": [...]}. */
public record GraphQLResponse(DataWrapper data, List<Map<String, Object>> errors) {

    public record DataWrapper(SearchResult search) {
    }
}
