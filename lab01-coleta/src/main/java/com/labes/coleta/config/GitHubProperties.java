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
}
