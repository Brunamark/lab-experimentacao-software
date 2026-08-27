package com.labes.coleta.client;

import com.labes.coleta.config.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente da API REST do GitHub, usado apenas para contornar uma limitação do GraphQL:
 * {@code releases.totalCount} satura em 1000, então repositórios com mais releases que isso
 * vêm truncados (ex.: llama.cpp devolve 1000, mas tem 6850).
 *
 * <p>A contagem real é obtida pedindo uma release por página e lendo o número da última
 * página no cabeçalho {@code Link} — com {@code per_page=1}, última página = total de releases.
 *
 * <p>Continua sendo script próprio do grupo: nenhuma biblioteca de terceiros para consultar
 * a API, só {@code RestTemplate} do Spring.
 */
@Component
public class GitHubRestClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubRestClient.class);

    /** Casa o trecho {@code &page=6850>; rel="last"} do cabeçalho Link. */
    private static final Pattern ULTIMA_PAGINA =
            Pattern.compile("[?&]page=(\\d+)>;\\s*rel=\"last\"");

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;

    public GitHubRestClient(RestTemplateBuilder builder, GitHubProperties properties) {
        this.restTemplate = builder.build();
        this.properties = properties;
    }

    /**
     * Conta as releases de um repositório.
     *
     * @param nameWithOwner formato "owner/nome"
     * @param valorSeFalhar valor devolvido quando a API não responde — preferimos manter o
     *                      número do GraphQL a inventar um dado
     */
    public int contarReleases(String nameWithOwner, int valorSeFalhar) {
        String[] partes = nameWithOwner.split("/", 2);
        if (partes.length != 2) {
            log.warn("Nome de repositório em formato inesperado: {}", nameWithOwner);
            return valorSeFalhar;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getToken());

        try {
            ResponseEntity<String> resposta = restTemplate.exchange(
                    properties.getRest().url() + "/repos/{owner}/{nome}/releases?per_page=1",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class,
                    partes[0], partes[1]);

            String link = resposta.getHeaders().getFirst(HttpHeaders.LINK);

            if (link == null) {
                // Sem cabeçalho Link não há paginação: no máximo uma release no corpo.
                String corpo = resposta.getBody();
                return (corpo == null || corpo.isBlank() || corpo.strip().equals("[]")) ? 0 : 1;
            }

            Matcher matcher = ULTIMA_PAGINA.matcher(link);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

            log.warn("Cabeçalho Link sem rel=\"last\" para {}: {}", nameWithOwner, link);
            return valorSeFalhar;

        } catch (RestClientException e) {
            log.warn("Falha ao contar releases de {} via REST ({}). Mantendo o valor do GraphQL.",
                    nameWithOwner, e.getMessage());
            return valorSeFalhar;
        }
    }
}
