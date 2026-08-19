package com.labes.coleta.dto;

import java.util.List;

/** Resultado de um único alias "search" que busca PRs (não só contagem). */
public record PullRequestSizeResult(List<PullRequestSizeNode> nodes) {
}
