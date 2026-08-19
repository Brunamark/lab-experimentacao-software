package com.labes.coleta.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.labes.coleta.metrics.CallLog;
import com.labes.coleta.metrics.MetricsCsvWriter;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class Hooks {

    /** Porta fixa: precisa bater com github.graphql-url/rest-base-url em application-test.yml. */
    private static final int PORTA_WIREMOCK = 8089;
    private static final WireMockServer WIREMOCK = new WireMockServer(options().port(PORTA_WIREMOCK));
    private static volatile boolean iniciado = false;

    private final CallLog callLog;
    private final MetricsCsvWriter metricsCsvWriter;
    private final ContextoCenario contexto;

    public Hooks(CallLog callLog, MetricsCsvWriter metricsCsvWriter, ContextoCenario contexto) {
        this.callLog = callLog;
        this.metricsCsvWriter = metricsCsvWriter;
        this.contexto = contexto;
    }

    @Before(order = 0)
    public void prepararAmbiente(Scenario scenario) {
        iniciarWireMockSeNecessario();
        WIREMOCK.resetAll();
        callLog.reset();
        contexto.reset();
        contexto.nomeCenario = scenario.getName();
        contexto.nomeFeature = extrairNomeFeature(scenario.getUri().toString());
    }

    @After
    public void exportarMetricasGenericas(Scenario scenario) {
        String feature = extrairNomeFeature(scenario.getUri().toString());
        int total = callLog.totalChamadas();
        long unicas = callLog.chamadasUnicas();
        long redundantes = callLog.chamadasRedundantes();
        long retries = callLog.tentativasDeRetry();
        double taxaRedundancia = total == 0 ? 0.0 : (double) redundantes / total;

        metricsCsvWriter.escrever(feature, scenario.getName(), "total_chamadas", total);
        metricsCsvWriter.escrever(feature, scenario.getName(), "chamadas_unicas", unicas);
        metricsCsvWriter.escrever(feature, scenario.getName(), "chamadas_redundantes", redundantes);
        metricsCsvWriter.escrever(feature, scenario.getName(), "tentativas_de_retry", retries);
        metricsCsvWriter.escrever(feature, scenario.getName(), "taxa_redundancia", taxaRedundancia);
    }

    private static synchronized void iniciarWireMockSeNecessario() {
        if (!iniciado) {
            WIREMOCK.start();
            iniciado = true;
            Runtime.getRuntime().addShutdownHook(new Thread(WIREMOCK::stop));
        }
    }

    private String extrairNomeFeature(String uri) {
        String semBarra = uri.substring(uri.lastIndexOf('/') + 1);
        return semBarra.replace(".feature", "");
    }

    static WireMockServer servidor() {
        return WIREMOCK;
    }
}
