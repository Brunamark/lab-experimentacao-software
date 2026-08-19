package com.labes.coleta.cucumber;

import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.metrics.CallLog;
import com.labes.coleta.metrics.MetricsCsvWriter;
import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.service.ColetaService;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Quando;
import io.cucumber.java.pt.Então;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ColetaSteps {

    private final ContextoCenario ctx;

    // campos que estavam faltando
    private final GitHubProperties properties;
    private final ColetaService coletaService;
    private final MetricsCsvWriter metricsCsvWriter;
    private final CallLog callLog;
    private final Optional<WireMockStubs> wireMockStubs;
    private final Optional<WireMockServerProvider> wireMockServerProvider;

    public ColetaSteps(
            ContextoCenario ctx,
            GitHubProperties properties,
            ColetaService coletaService,
            MetricsCsvWriter metricsCsvWriter,
            CallLog callLog,
            @Autowired(required = false) WireMockStubs wireMockStubs,
            @Autowired(required = false) WireMockServerProvider wireMockServerProvider
    ) {
        this.ctx = ctx;
        this.properties = properties;
        this.coletaService = coletaService;
        this.metricsCsvWriter = metricsCsvWriter;
        this.callLog = callLog;
        this.wireMockStubs = Optional.ofNullable(wireMockStubs);
        this.wireMockServerProvider = Optional.ofNullable(wireMockServerProvider);
    }

    // helper opcional para steps que exigem mock
    private WireMockStubs requireWireMock() {
        return wireMockStubs.orElseThrow(() ->
                new IllegalStateException("Este step depende de WireMock (perfil mock)."));
    }

    // ---- Dado / E: preparação comum ----

    @Dado("que a API do GitHub tem {int} repositórios disponíveis para a busca")
    public void que_a_api_do_github_tem_repositorios_disponiveis(int totalDisponivel) {
        ctx.totalDisponivel = totalDisponivel;
        properties.setTotalRepos(totalDisponivel);
    }

    @Dado("o total de repositórios para coleta é {int}")
    public void o_total_de_repositorios_para_coleta_e(int totalRepos) {
        if (totalRepos <= 0) {
            throw new IllegalArgumentException("totalRepos deve ser > 0");
        }
        ctx.totalDisponivel = totalRepos;
        properties.setTotalRepos(totalRepos);
    }

    @E("o tamanho de página configurado é {int}")
    public void o_tamanho_de_pagina_configurado_e(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize deve ser > 0");
        }
        ctx.pageSize = pageSize;
        properties.setPageSize(pageSize); // <- garante uso do valor do cenário (ex.: 2)

        // Só executa em perfil mock (quando o bean existe)
        wireMockStubs.ifPresent(stubs -> {
            // stubs.algumSetupRelacionadoAoPageSize(pageSize); // se houver
        });
    }

    @E("a primeira tentativa da página vai falhar com erro {int}")
    public void a_primeira_tentativa_da_pagina_vai_falhar_com_erro(int codigoDeErro) {
        wireMockStubs.ifPresent(stubs ->
                stubs.stubPaginacaoComFalhaTransitoriaNaPrimeiraPagina(ctx.totalDisponivel, ctx.pageSize, codigoDeErro));
        ctx.stubConfiguradoManualmente = true;
    }

    // ---- Quando ----

    @Quando("a coleta é executada")
    public void a_coleta_e_executada() {
        garantirStubDePaginacaoPadrao();
        try {
            ctx.resultado1 = coletaService.coletar();
        } catch (RuntimeException e) {
            ctx.excecaoLancada = e;
        }
    }

    @Quando("a coleta é executada duas vezes seguidas com os mesmos parâmetros")
    public void a_coleta_e_executada_duas_vezes_seguidas() {
        garantirStubDePaginacaoPadrao();
        ctx.resultado1 = coletaService.coletar();
        wireMockServerProvider.ifPresent(provider -> provider.resetScenarios());
        ctx.resultado2 = coletaService.coletar();
    }

    private void garantirStubDePaginacaoPadrao() {
        if (!ctx.stubConfiguradoManualmente) {
            wireMockStubs.ifPresent(stubs -> {
                stubs.stubPaginacao(ctx.totalDisponivel, ctx.pageSize);
            });
        }
    }

    // ---- Então ----

    @Então("devem ser coletados ao menos {int} repositórios")
    public void devem_ser_coletados_ao_menos_repositorios(int minimo) {
        assertThat(ctx.resultado1).isNotNull();
        assertThat(ctx.resultado1.size()).isGreaterThanOrEqualTo(minimo);
    }

    @Então("a cobertura de paginação deve ser de {int}%")
    public void a_cobertura_de_paginacao_deve_ser(int percentualEsperado) {
        double cobertura = 100.0 * ctx.resultado1.size() / ctx.totalDisponivel;
        metricsCsvWriter.escrever(ctx.nomeFeature, nomeCenarioAtual(), "cobertura_paginacao_pct", cobertura);
        assertThat(cobertura).isEqualTo((double) percentualEsperado);
    }

    @Então("cada cursor de página deve ter sido usado exatamente uma vez")
    public void cada_cursor_de_pagina_deve_ter_sido_usado_exatamente_uma_vez() {
        long paginasEsperadas = (long) Math.ceil(ctx.totalDisponivel / (double) ctx.pageSize);
        long chamadasUnicas = callLog.chamadasUnicasPara("/graphql");
        assertThat(chamadasUnicas).isEqualTo(paginasEsperadas);
    }

    @Então("nenhuma requisição deve ser feita após {string} ser falso")
    public void nenhuma_requisicao_deve_ser_feita_apos_hasnextpage_ser_falso(String campo) {
        long totalGraphQL = callLog.chamadasPara("/graphql");
        long unicasGraphQL = callLog.chamadasUnicasPara("/graphql");

        // ignora REST; verifica apenas repetição indevida de paginação GraphQL
        assertThat(totalGraphQL - unicasGraphQL).isEqualTo(0L);
    }

    @Então("nenhuma chamada redundante deve ser registrada")
    public void nenhuma_chamada_redundante_deve_ser_registrada() {
        metricsCsvWriter.escrever(ctx.nomeFeature, nomeCenarioAtual(),
                "chamadas_redundantes", callLog.chamadasRedundantes());
        assertThat(callLog.chamadasRedundantes()).isZero();
    }

    @Então("a coleta deve ter sucesso mesmo assim")
    public void a_coleta_deve_ter_sucesso_mesmo_assim() {
        assertThat(ctx.excecaoLancada).isNull();
        assertThat(ctx.resultado1).isNotNull();
    }

    @Então("deve haver exatamente {int} tentativa de retry")
    public void deve_haver_exatamente_uma_tentativa_de_retry(int esperado) {
        assertThat(callLog.tentativasDeRetry()).isEqualTo(esperado);
    }

    @Então("os dois resultados devem ser idênticos")
    public void os_dois_resultados_devem_ser_identicos() {
        assertThat(ctx.resultado1).isEqualTo(ctx.resultado2);
    }

    @Então("a taxa de idempotência deve ser de {int}%")
    public void a_taxa_de_idempotencia_deve_ser(int percentualEsperado) {
        List<RepositorioMetrica> r1 = ctx.resultado1;
        List<RepositorioMetrica> r2 = ctx.resultado2;
        long iguais = Math.min(r1.size(), r2.size());
        // conta quantos itens, posição a posição, batem entre as duas execuções
        long divergentes = 0;
        for (int i = 0; i < iguais; i++) {
            if (!r1.get(i).equals(r2.get(i))) {
                divergentes++;
            }
        }
        double taxaIdempotencia = iguais == 0 ? 0.0 : 100.0 * (iguais - divergentes) / iguais;
        metricsCsvWriter.escrever(ctx.nomeFeature, nomeCenarioAtual(), "taxa_idempotencia_pct", taxaIdempotencia);
        assertThat(taxaIdempotencia).isEqualTo((double) percentualEsperado);
    }

    private String nomeCenarioAtual() {
        return ctx.nomeCenario;
    }
}
