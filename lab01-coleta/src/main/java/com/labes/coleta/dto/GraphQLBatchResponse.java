package com.labes.coleta.dto;

import java.util.List;
import java.util.Map;

/** Envelope de resposta para queries com múltiplos aliases "search" (ex.: contagens em lote). */
public record GraphQLBatchResponse(Map<String, IssueCountResult> data, List<Map<String, Object>> errors) {
}
