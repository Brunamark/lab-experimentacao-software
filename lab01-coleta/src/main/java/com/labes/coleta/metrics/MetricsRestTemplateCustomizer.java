package com.labes.coleta.metrics;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * O Spring Boot aplica automaticamente todo bean {@link RestTemplateCustomizer} encontrado
 * no contexto a qualquer RestTemplate construído via {@code RestTemplateBuilder} —
 * incluindo os dos clients de produção (GitHubGraphQLClient, GitHubRestClient), sem
 * precisar alterar o código deles para os testes.
 */
@Component
public class MetricsRestTemplateCustomizer implements RestTemplateCustomizer {

    private final CallLog callLog;

    public MetricsRestTemplateCustomizer(CallLog callLog) {
        this.callLog = callLog;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(new MetricsInterceptor(callLog));
    }
}
