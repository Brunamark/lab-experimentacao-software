package com.labes.coleta.client;

import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.GraphQLBatchResponse;
import com.labes.coleta.dto.GraphQLRequest;
import com.labes.coleta.dto.GraphQLResponse;
import com.labes.coleta.dto.GraphQLSizeBatchResponse;
import com.labes.coleta.dto.IssueCountResult;
import com.labes.coleta.dto.PullRequestSizeResult;
import com.labes.coleta.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Responsável apenas por falar com a API GraphQL do GitHub: monta a requisição,
 * envia e devolve o "search" já desserializado. Nenhuma lib de terceiros para
 * consumo da API é usada — só RestTemplate (Spring) e Jackson (parsing JSON,
 * que já vem embutido no starter-web).
 */
@Component
public class GitHubGraphQLClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubGraphQLClient.class);

    /**
     * A API do GitHub devolve 502/504 de forma intermitente em queries com conexões aninhadas
     * (releases, pullRequests, issues). São falhas transitórias: repetir a mesma requisição
     * costuma funcionar, então sem retry qualquer coleta longa morre no meio.
     */
    private static final int MAX_TENTATIVAS = 5;
    private static final long ESPERA_INICIAL_MS = 2_000;

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final String queryTemplate;

    public GitHubGraphQLClient(RestTemplateBuilder builder, GitHubProperties properties) throws IOException {
        this.restTemplate = builder.build();
        this.properties = properties;
        this.queryTemplate = carregarQuery();
    }

    /** Carrega o texto da query GraphQL do arquivo em resources/graphql. */
    private String carregarQuery() throws IOException {
        ClassPathResource resource = new ClassPathResource("graphql/busca-repositorios.graphql");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Busca uma página de repositórios.
     *
     * @param queryString filtro de busca do GitHub (ex.: "stars:>1 sort:stars-desc")
     * @param quantidade  quantos repositórios pedir nessa página (máx. 100)
     * @param cursor      cursor de paginação (null na primeira página)
     */
    public SearchResult buscarPagina(String queryString, int quantidade, String cursor) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("queryString", queryString);
        variables.put("qtd", quantidade);
        variables.put("cursor", cursor);

        GraphQLRequest requestBody = new GraphQLRequest(queryTemplate, variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getToken());

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(requestBody, headers);

        return executarComRetry(() -> enviar(entity), "Cursor da página: " + cursor);
    }

    /**
     * Executa {@code operacao} com retry e backoff exponencial em caso de erro 5xx/rede —
     * mesma proteção usada em {@link #buscarPagina}, reaproveitada pelas buscas em lote,
     * que sofrem do mesmo problema de 502/504 intermitente em queries com conexões aninhadas.
     */
    private <T> T executarComRetry(Supplier<T> operacao, String contexto) {
        long espera = ESPERA_INICIAL_MS;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return operacao.get();
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (tentativa == MAX_TENTATIVAS) {
                    throw new IllegalStateException(
                            "API do GitHub falhou nas %d tentativas. %s"
                                    .formatted(MAX_TENTATIVAS, contexto), e);
                }
                log.warn("Falha transitória da API ({}). Tentativa {}/{} — repetindo em {} ms.",
                        e.getClass().getSimpleName(), tentativa, MAX_TENTATIVAS, espera);
                aguardar(espera);
                espera *= 2;
            }
        }

        throw new IllegalStateException("Fluxo inalcançável: laço de tentativas encerrado sem resultado.");
    }

    /** Envia a requisição e valida a resposta. Erros 5xx sobem para o laço de retry. */
    private SearchResult enviar(HttpEntity<GraphQLRequest> entity) {
        ResponseEntity<GraphQLResponse> response =
                restTemplate.postForEntity(properties.getGraphql().url(), entity, GraphQLResponse.class);

        GraphQLResponse body = response.getBody();

        if (body == null || body.data() == null) {
            throw new IllegalStateException("Resposta vazia ou inválida da API do GitHub.");
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new IllegalStateException("Erro retornado pela API GraphQL: " + body.errors());
        }

        return body.data().search();
    }

    private void aguardar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Coleta interrompida durante a espera do retry.", e);
        }
    }

    /**
     * Executa uma query já montada com múltiplos aliases "search" (ex.: contagens de PRs
     * em lote) e devolve o resultado de cada alias. Diferente de {@link #buscarPagina},
     * a query aqui não vem de um arquivo fixo: é montada dinamicamente por quem chama,
     * porque o número de aliases varia a cada chamada.
     */
    public Map<String, IssueCountResult> buscarContagensEmLote(String queryMontada) {
        GraphQLRequest requestBody = new GraphQLRequest(queryMontada, Map.of());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getToken());

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(requestBody, headers);

        return executarComRetry(() -> enviarLoteContagens(entity), "Busca de contagens em lote.");
    }

    private Map<String, IssueCountResult> enviarLoteContagens(HttpEntity<GraphQLRequest> entity) {
        ResponseEntity<GraphQLBatchResponse> response =
            restTemplate.postForEntity(properties.getGraphql().url(), entity, GraphQLBatchResponse.class);

        GraphQLBatchResponse body = response.getBody();

        if (body == null || body.data() == null) {
            throw new IllegalStateException("Resposta vazia ou inválida da API do GitHub.");
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new IllegalStateException("Erro retornado pela API GraphQL: " + body.errors());
        }

        return body.data();
    }

    /**
     * Como {@link #buscarContagensEmLote}, mas para queries que retornam PRs de verdade
     * (nodes com additions/deletions), não só uma contagem.
     */
    public Map<String, PullRequestSizeResult> buscarTamanhosEmLote(String queryMontada) {
        GraphQLRequest requestBody = new GraphQLRequest(queryMontada, Map.of());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getToken());

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(requestBody, headers);

        return executarComRetry(() -> enviarLoteTamanhos(entity), "Busca de tamanhos de PRs em lote.");
    }

    private Map<String, PullRequestSizeResult> enviarLoteTamanhos(HttpEntity<GraphQLRequest> entity) {
        ResponseEntity<GraphQLSizeBatchResponse> response =
            restTemplate.postForEntity(properties.getGraphql().url(), entity, GraphQLSizeBatchResponse.class);

        GraphQLSizeBatchResponse body = response.getBody();

        if (body == null || body.data() == null) {
            throw new IllegalStateException("Resposta vazia ou inválida da API do GitHub.");
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new IllegalStateException("Erro retornado pela API GraphQL: " + body.errors());
        }

        return body.data();
    }
}
