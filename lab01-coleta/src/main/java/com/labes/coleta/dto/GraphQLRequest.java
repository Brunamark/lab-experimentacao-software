package com.labes.coleta.dto;

import java.util.Map;

/** Corpo da requisição POST enviada para https://api.github.com/graphql. */
public record GraphQLRequest(String query, Map<String, Object> variables) {
}
