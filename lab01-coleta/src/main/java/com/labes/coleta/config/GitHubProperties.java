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
    private int totalRepos = 100;
    private int pageSize = 50;
    private int anoInicioAnalise = 2022;
    private int limiteRepositoriosAnalisePrs = 100;
    private int amostraPrsTamanho = 10;

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

    public record ApiProperties(String url) {
    }
}
