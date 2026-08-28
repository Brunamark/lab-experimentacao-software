package com.labes.coleta.dto;

import java.util.List;
import java.util.Map;

/** Envelope de resposta para queries com múltiplos aliases "search" que retornam issues. */
public record GraphQLIssueCommitsBatchResponse(
        Map<String, IssueCommitsResult> data,
        List<Map<String, Object>> errors) {
}
