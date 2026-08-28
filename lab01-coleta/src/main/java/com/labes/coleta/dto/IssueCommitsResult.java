package com.labes.coleta.dto;

import java.util.List;

/**
 * Resultado de um alias "search" da coleta de commits por issue (RQB03).
 *
 * <p>Serve aos dois tipos de alias da mesma query: os de contagem, que só pedem
 * {@code issueCount} e vêm com {@code nodes} nulo, e os de amostra, que trazem os dois.
 */
public record IssueCommitsResult(int issueCount, List<IssueCommitsNode> nodes) {
}
