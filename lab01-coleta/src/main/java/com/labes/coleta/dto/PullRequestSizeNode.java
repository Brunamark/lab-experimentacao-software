package com.labes.coleta.dto;

/** Um PR individual retornado dentro de um alias "search" (fragmento ... on PullRequest). */
public record PullRequestSizeNode(Integer additions, Integer deletions) {
}
