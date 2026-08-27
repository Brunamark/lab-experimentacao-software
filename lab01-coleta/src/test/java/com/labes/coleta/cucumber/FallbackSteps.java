package com.labes.coleta.cucumber;

import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.metrics.CallLog;
import com.labes.coleta.metrics.MetricsCsvWriter;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class FallbackSteps {

    @Autowired
    private GitHubProperties properties;
    @Autowired
    private WireMockStubs wireMockStubs;
    @Autowired
    private CallLog callLog;
    @Autowired
    private MetricsCsvWriter metricsCsvWriter;
    @Autowired
    private ContextoCenario contexto;

    @Dado("um repositório cujo total de releases no GraphQL vem saturado em {int}")
    public void um_repositorio_cujo_total_de_releases_vem_saturado(int teto) {
        contexto.nameWithOwner = "org/repo-saturado";
        properties.setTotalRepos(1);
        properties.setPageSize(1);
        contexto.totalDisponivel = 1;
        contexto.pageSize = 1;
        wireMockStubs.stubUmRepositorio(contexto.nameWithOwner, teto);
        contexto.stubConfiguradoManualmente = true;
    }

    @Dado("um repositório com {int} releases no GraphQL")
    public void um_repositorio_com_releases_no_graphql(int totalReleases) {
        contexto.nameWithOwner = "org/repo-normal";
        properties.setTotalRepos(1);
        properties.setPageSize(1);
        contexto.totalDisponivel = 1;
        contexto.pageSize = 1;
        wireMockStubs.stubUmRepositorio(contexto.nameWithOwner, totalReleases);
        contexto.stubConfiguradoManualmente = true;
    }

    @E("a API REST informa que o total real de releases é {int}")
    public void a_api_rest_informa_que_o_total_real_de_releases_e(int totalReal) {
        wireMockStubs.stubRestReleases(contexto.nameWithOwner, totalReal);
    }

    @E("a API REST de releases está indisponível")
    public void a_api_rest_de_releases_esta_indisponivel() {
        wireMockStubs.stubRestReleasesIndisponivel(contexto.nameWithOwner);
    }

    @Então("o total de releases do repositório deve ser {int}")
    public void o_total_de_releases_do_repositorio_deve_ser(int totalEsperado) {
        assertThat(contexto.resultado1).hasSize(1);
        assertThat(contexto.resultado1.get(0).totalReleases()).isEqualTo(totalEsperado);
    }

    @Então("o fallback via API REST deve ter sido acionado")
    public void o_fallback_via_api_rest_deve_ter_sido_acionado() {
        long chamadasRest = callLog.chamadasPara("/releases");
        metricsCsvWriter.escrever(contexto.nomeFeature, contexto.nomeCenario, "fallback_acionado", chamadasRest > 0);
        assertThat(chamadasRest).isGreaterThan(0);
    }

    @Então("o fallback via API REST não deve ter sido acionado")
    public void o_fallback_via_api_rest_nao_deve_ter_sido_acionado() {
        long chamadasRest = callLog.chamadasPara("/releases");
        metricsCsvWriter.escrever(contexto.nomeFeature, contexto.nomeCenario, "fallback_acionado", chamadasRest > 0);
        assertThat(chamadasRest).isZero();
    }
}
