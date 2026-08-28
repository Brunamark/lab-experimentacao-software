package com.labes.coleta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mapeia as propriedades "github.*" do application.yml
 */
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    private String token;
    private ApiProperties graphql = new ApiProperties("https://api.github.com/graphql");
    private ApiProperties rest = new ApiProperties("https://api.github.com");
    private int totalRepos = 10;
    private int pageSize = 50;
    private int anoInicioAnalise = 2022;
    private int limiteRepositoriosAnalisePrs = 10;
    private int amostraPrsTamanho = 10;
    private int amostraIssuesCommits = 10;
    private boolean coletarTendenciaPrs = true;
    private boolean coletarTamanhoPrs = true;
    private boolean coletarCommitsPorIssue = true;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ApiProperties getGraphql() {
        return graphql;
    }

    public void setGraphql(ApiProperties graphql) {
        this.graphql = graphql;
    }

    public ApiProperties getRest() {
        return rest;
    }

    public void setRest(ApiProperties rest) {
        this.rest = rest;
    }

    public int getTotalRepos() {
        return totalRepos;
    }

    public void setTotalRepos(int totalRepos) {
        this.totalRepos = totalRepos;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getAnoInicioAnalise() {
        return anoInicioAnalise;
    }

    public void setAnoInicioAnalise(int anoInicioAnalise) {
        this.anoInicioAnalise = anoInicioAnalise;
    }

    public int getLimiteRepositoriosAnalisePrs() {
        return limiteRepositoriosAnalisePrs;
    }

    public void setLimiteRepositoriosAnalisePrs(int limiteRepositoriosAnalisePrs) {
        this.limiteRepositoriosAnalisePrs = limiteRepositoriosAnalisePrs;
    }

    public int getAmostraPrsTamanho() {
        return amostraPrsTamanho;
    }

    public void setAmostraPrsTamanho(int amostraPrsTamanho) {
        this.amostraPrsTamanho = amostraPrsTamanho;
    }

    public int getAmostraIssuesCommits() {
        return amostraIssuesCommits;
    }

    public void setAmostraIssuesCommits(int amostraIssuesCommits) {
        this.amostraIssuesCommits = amostraIssuesCommits;
    }

    public boolean isColetarTendenciaPrs() {
        return coletarTendenciaPrs;
    }

    public void setColetarTendenciaPrs(boolean coletarTendenciaPrs) {
        this.coletarTendenciaPrs = coletarTendenciaPrs;
    }

    public boolean isColetarTamanhoPrs() {
        return coletarTamanhoPrs;
    }

    public void setColetarTamanhoPrs(boolean coletarTamanhoPrs) {
        this.coletarTamanhoPrs = coletarTamanhoPrs;
    }

    public boolean isColetarCommitsPorIssue() {
        return coletarCommitsPorIssue;
    }

    public void setColetarCommitsPorIssue(boolean coletarCommitsPorIssue) {
        this.coletarCommitsPorIssue = coletarCommitsPorIssue;
    }

    public record ApiProperties(String url) {
    }
}
