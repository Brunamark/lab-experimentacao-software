package com.labes.coleta.client;

import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.GraphQLRequest;
import com.labes.coleta.dto.GraphQLResponse;
import com.labes.coleta.dto.SearchResult;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsável apenas por falar com a API GraphQL do GitHub: monta a requisição,
 * envia e devolve o "search" já desserializado. Nenhuma lib de terceiros para
 * consumo da API é usada — só RestTemplate (Spring) e Jackson (parsing JSON,
 * que já vem embutido no starter-web).
 */
@Component
public class GitHubGraphQLClient {

    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

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

        ResponseEntity<GraphQLResponse> response =
                restTemplate.postForEntity(GRAPHQL_URL, entity, GraphQLResponse.class);

        GraphQLResponse body = response.getBody();

        if (body == null || body.data() == null) {
            throw new IllegalStateException("Resposta vazia ou inválida da API do GitHub.");
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new IllegalStateException("Erro retornado pela API GraphQL: " + body.errors());
        }

        return body.data().search();
    }
}
