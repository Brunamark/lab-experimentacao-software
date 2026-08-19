package com.labes.coleta.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestComponent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

/**
 * Constrói os stubs HTTP que simulam a API do GitHub. Usa o recurso de "Scenarios" do
 * WireMock (estado interno por chamada) em vez de casar o corpo exato da requisição —
 * mais simples e mais fiel ao que realmente queremos simular: "a 1ª chamada devolve a
 * página 1, a 2ª devolve a página 2 ...", incluindo falhas no meio do caminho.
 */
@TestComponent
public class WireMockStubs {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CENARIO_PAGINACAO = "paginacao-graphql";

    private final WireMockServerProvider wireMockServerProvider;

    public WireMockStubs(WireMockServerProvider wireMockServerProvider) {
        this.wireMockServerProvider = wireMockServerProvider;
    }

    private WireMockServer wireMock() {
        return wireMockServerProvider.get();
    }

    /**
     * Simula {@code totalNoServidor} repositórios disponíveis na busca, devolvidos em
     * páginas de tamanho {@code pageSize}, na ordem correta, sem repetir cursor.
     */
    public void stubPaginacao(int totalNoServidor, int pageSize) {
        int paginas = (int) Math.ceil(totalNoServidor / (double) pageSize);
        String estadoAtual = STARTED;

        for (int pagina = 0; pagina < paginas; pagina++) {
            int jaEnviados = pagina * pageSize;
            int qtdNestaPagina = Math.min(pageSize, totalNoServidor - jaEnviados);
            boolean temProxima = (jaEnviados + qtdNestaPagina) < totalNoServidor;
            String cursor = "cursor-pagina-" + (pagina + 1);
            String proximoEstado = "pagina-" + (pagina + 1) + "-entregue";

            wireMock().stubFor(post(urlEqualTo("/graphql"))
                    .inScenario(CENARIO_PAGINACAO)
                    .whenScenarioStateIs(estadoAtual)
                    .willReturn(okJson(corpoRespostaPagina(totalNoServidor, jaEnviados, qtdNestaPagina,
                            temProxima, cursor)))
                    .willSetStateTo(proximoEstado));

            estadoAtual = proximoEstado;
        }
    }

    /**
     * Igual a {@link #stubPaginacao}, mas a primeira tentativa da primeira página
     * responde 502 antes de suceder — simula a falha transitória que o retry do
     * GitHubGraphQLClient precisa absorver.
     */
    public void stubPaginacaoComFalhaTransitoriaNaPrimeiraPagina(int totalNoServidor, int pageSize,
                                                                   int codigoDeErro) {
        wireMock().stubFor(post(urlEqualTo("/graphql"))
                .inScenario(CENARIO_PAGINACAO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(codigoDeErro))
                .willSetStateTo("apos-falha-transitoria"));

        int paginas = (int) Math.ceil(totalNoServidor / (double) pageSize);
        String estadoAtual = "apos-falha-transitoria";

        for (int pagina = 0; pagina < paginas; pagina++) {
            int jaEnviados = pagina * pageSize;
            int qtdNestaPagina = Math.min(pageSize, totalNoServidor - jaEnviados);
            boolean temProxima = (jaEnviados + qtdNestaPagina) < totalNoServidor;
            String cursor = "cursor-pagina-" + (pagina + 1);
            String proximoEstado = "pagina-" + (pagina + 1) + "-entregue";

            wireMock().stubFor(post(urlEqualTo("/graphql"))
                    .inScenario(CENARIO_PAGINACAO)
                    .whenScenarioStateIs(estadoAtual)
                    .willReturn(okJson(corpoRespostaPagina(totalNoServidor, jaEnviados, qtdNestaPagina,
                            temProxima, cursor)))
                    .willSetStateTo(proximoEstado));

            estadoAtual = proximoEstado;
        }
    }

    /** Simula um único repositório, útil pros cenários de fallback (total=1, pageSize=1). */
    public void stubUmRepositorio(String nameWithOwner, int totalReleasesNoGraphQL) {
        String corpo = corpoComNode(1, List.of(node(nameWithOwner, 100, totalReleasesNoGraphQL)),
                false, "cursor-unico");
        wireMock().stubFor(post(urlEqualTo("/graphql")).willReturn(okJson(corpo)));
    }

    /** Simula a API REST de releases respondendo com o total real via header Link. */
    public void stubRestReleases(String nameWithOwner, int totalReal) {
        String[] partes = nameWithOwner.split("/", 2);
        String link = "<https://api.github.com/repos/%s/%s/releases?per_page=1&page=%d>; rel=\"last\""
                .formatted(partes[0], partes[1], totalReal);
        wireMock().stubFor(get(urlPathEqualTo("/repos/%s/%s/releases".formatted(partes[0], partes[1])))
                .willReturn(aResponse().withStatus(200).withHeader("Link", link).withBody("[{}]")));
    }

    public void stubRestReleasesIndisponivel(String nameWithOwner) {
        String[] partes = nameWithOwner.split("/", 2);
        wireMock().stubFor(get(urlPathEqualTo("/repos/%s/%s/releases".formatted(partes[0], partes[1])))
                .willReturn(aResponse().withStatus(500)));
    }

    // ---- montagem do JSON de resposta (formato do GraphQLResponse/SearchResult) ----

    private String corpoRespostaPagina(int totalNoServidor, int jaEnviados, int qtd, boolean temProxima,
                                        String cursor) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (int i = 0; i < qtd; i++) {
            int indice = jaEnviados + i + 1;
            nodes.add(node("org/repo-" + indice, totalNoServidor - indice, 10));
        }
        return corpoComNode(totalNoServidor, nodes, temProxima, cursor);
    }

    private String corpoComNode(int repositoryCount, List<Map<String, Object>> nodes, boolean hasNextPage,
                                 String endCursor) {
        try {
            Map<String, Object> pageInfo = new LinkedHashMap<>();
            pageInfo.put("hasNextPage", hasNextPage);
            pageInfo.put("endCursor", endCursor);

            Map<String, Object> search = new LinkedHashMap<>();
            search.put("repositoryCount", repositoryCount);
            search.put("pageInfo", pageInfo);
            search.put("nodes", nodes);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("search", search);

            Map<String, Object> raiz = new LinkedHashMap<>();
            raiz.put("data", data);
            raiz.put("errors", null);

            return JSON.writeValueAsString(raiz);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao montar fixture JSON de teste", e);
        }
    }

    private Map<String, Object> node(String nameWithOwner, int estrelas, int totalReleases) {
        Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> releases = new LinkedHashMap<>();
        releases.put("totalCount", totalReleases);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("nameWithOwner", nameWithOwner);
        node.put("stargazerCount", estrelas);
        node.put("createdAt", agora.minusSeconds(60L * 60 * 24 * 365 * 3).toString());
        node.put("updatedAt", agora.toString());
        node.put("pushedAt", agora.toString());
        node.put("releases", releases);
        return node;
    }
}
