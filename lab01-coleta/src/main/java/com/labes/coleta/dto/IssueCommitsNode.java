package com.labes.coleta.dto;

import java.util.List;

/**
 * Uma issue fechada, trazendo os Pull Requests que a fecharam (RQB03).
 *
 * <p>Usamos {@code closedByPullRequestsReferences} e não o {@code closer} do
 * {@code ClosedEvent} do timeline: medido contra a API real, o campo do timeline só vem
 * preenchido quando o fechamento foi automático, o que atribuía 15 de 20 issues onde este
 * atribui 19 de 20.
 */
public record IssueCommitsNode(ClosedByPullRequests closedByPullRequestsReferences) {

    /** PRs que fecharam a issue — mais de um quando a resolução foi partida em vários PRs. */
    public record ClosedByPullRequests(int totalCount, List<PullRequest> nodes) {
    }

    public record PullRequest(Commits commits) {

        public record Commits(int totalCount) {
        }
    }
}
