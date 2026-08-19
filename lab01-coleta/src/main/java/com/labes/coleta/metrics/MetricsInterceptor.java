package com.labes.coleta.metrics;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Intercepta toda requisição feita pelo RestTemplate (tanto do GitHubGraphQLClient quanto
 * do GitHubRestClient) e registra no {@link CallLog}, sem alterar o comportamento real
 * da chamada. Registrado apenas em teste, via {@code MetricsRestTemplateCustomizer}.
 */
public class MetricsInterceptor implements ClientHttpRequestInterceptor {

    private final CallLog callLog;

    public MetricsInterceptor(CallLog callLog) {
        this.callLog = callLog;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String assinatura = request.getMethod() + " " + request.getURI() + "|"
                + new String(body, StandardCharsets.UTF_8);

        try {
            ClientHttpResponse response = execution.execute(request, body);
            boolean falhou = response.getStatusCode().isError();
            callLog.registrar(request.getMethod().name(), request.getURI().toString(), assinatura,
                    response.getStatusCode().value(), falhou);
            return response;
        } catch (IOException e) {
            // ResourceAccessException (timeout/conexão recusada) aparece aqui como IOException.
            callLog.registrar(request.getMethod().name(), request.getURI().toString(), assinatura, -1, true);
            throw e;
        }
    }
}
