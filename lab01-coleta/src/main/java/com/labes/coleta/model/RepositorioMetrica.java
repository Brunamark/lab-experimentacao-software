package com.labes.coleta.model;

import java.time.Instant;

/**
 * Dado coletado de um repositório, pronto para virar uma linha do CSV.
 *
 * @param ultimoPush           data do último push; null em repositório sem commits
 * @param diasDesdeUltimoPush  -1 quando não há data de push (sentinela de dado ausente)
 * @param linguagem            linguagem primária; string vazia quando o repositório não é código
 * @param issuesAbertas        issues abertas; não inclui pull requests
 * @param issuesFechadas       issues fechadas; não inclui pull requests
 * @param prsAceitas           pull requests com merge, de qualquer autor
 */
public record RepositorioMetrica(
        String nome,
        long estrelas,
        long idadeEmMeses,
        int totalReleases,
        Instant ultimoPush,
        Instant ultimaAtualizacao,
        long diasDesdeUltimoPush,
        // --- RQ05 ---
        String linguagem,
        // --- RQ06 ---
        int issuesAbertas,
        int issuesFechadas,
        // --- RQ02 ---
        int prsAceitas
) {

    /** Valor de {@code diasDesdeUltimoPush} quando o repositório não tem data de push. */
    public static final long SEM_DATA_DE_PUSH = -1;

    /** Rótulo dos repositórios sem linguagem primária, usado nos agrupamentos da RQ05/RQ07. */
    public static final String SEM_LINGUAGEM = "(sem linguagem)";

    /** Descarta da análise os repositórios sem data de push. */
    public boolean temDataDePush() {
        return diasDesdeUltimoPush != SEM_DATA_DE_PUSH;
    }

    /** Total de issues; 0 quando o repositório tem issues desabilitadas. */
    public int issuesTotais() {
        return issuesAbertas + issuesFechadas;
    }

    /** Descarta da RQ06 os repositórios sem nenhuma issue — a razão seria uma divisão por zero. */
    public boolean temIssues() {
        return issuesTotais() > 0;
    }

    /** Linguagem para fins de agrupamento, com rótulo explícito quando não há. */
    public String linguagemOuRotulo() {
        return linguagem == null || linguagem.isBlank() ? SEM_LINGUAGEM : linguagem;
    }

    /**
     * Cópia com o total de releases corrigido — o GraphQL satura em 1000 e o valor real
     * vem da API REST.
     *
     * <p>Centralizar aqui a chamada do construtor evita que cada ponto de cópia quebre
     * quando formos acrescentar um campo novo ao record.
     */
    public RepositorioMetrica comTotalReleases(int novoTotal) {
        return new RepositorioMetrica(nome, estrelas, idadeEmMeses, novoTotal,
                ultimoPush, ultimaAtualizacao, diasDesdeUltimoPush,
                linguagem, issuesAbertas, issuesFechadas, prsAceitas);
    }
}
