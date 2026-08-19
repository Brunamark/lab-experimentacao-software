package com.labes.coleta.dto;

import java.util.List;
import java.util.Map;

/** Envelope de resposta para queries com múltiplos aliases "search" que retornam PRs (não contagens). */
public record GraphQLSizeBatchResponse(Map<String, PullRequestSizeResult> data, List<Map<String, Object>> errors) {
}
