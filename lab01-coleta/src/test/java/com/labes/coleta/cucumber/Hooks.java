package com.labes.coleta.cucumber;

import com.labes.coleta.metrics.CallLog;
import com.labes.coleta.metrics.MetricsCsvWriter;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {

    private final CallLog callLog;
    private final MetricsCsvWriter metricsCsvWriter;
    private final ContextoCenario contexto;
    private final java.util.Optional<WireMockServerProvider> wireMockServerProvider;

    public Hooks(CallLog callLog, MetricsCsvWriter metricsCsvWriter, ContextoCenario contexto,
                 java.util.Optional<WireMockServerProvider> wireMockServerProvider) {
        this.callLog = callLog;
        this.metricsCsvWriter = metricsCsvWriter;
        this.contexto = contexto;
        this.wireMockServerProvider = wireMockServerProvider;
    }

    @Before(order = 0)
    public void prepararAmbiente(Scenario scenario) {
        wireMockServerProvider.ifPresent(provider -> {
            provider.start();
            provider.get().resetAll();
        });
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

    private String extrairNomeFeature(String uri) {
        String semBarra = uri.substring(uri.lastIndexOf('/') + 1);
        return semBarra.replace(".feature", "");
    }

}
