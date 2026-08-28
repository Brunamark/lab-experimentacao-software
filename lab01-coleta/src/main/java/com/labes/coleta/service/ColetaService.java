package com.labes.coleta.service;

import com.labes.coleta.client.GitHubGraphQLClient;
import com.labes.coleta.client.GitHubRestClient;
import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.RepositoryNode;
import com.labes.coleta.dto.SearchResult;
import com.labes.coleta.model.RepositorioMetrica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra a coleta: pagina os resultados da API até atingir o total
 * configurado (github.total-repos) e devolve a lista de repositórios coletados.
 */
@Service
public class ColetaService {

    private static final Logger log = LoggerFactory.getLogger(ColetaService.class);
    private static final String QUERY_STRING = "stars:>1 sort:stars-desc";

    /**
     * Valor em que o campo releases.totalCount do GraphQL satura. Quem chega nele pode ter
     * muito mais releases do que isso, e precisa ser recontado pela API REST.
     */
    private static final int TETO_RELEASES_GRAPHQL = 1000;

    private final GitHubGraphQLClient client;
    private final GitHubRestClient restClient;
    private final GitHubProperties properties;

    public ColetaService(GitHubGraphQLClient client, GitHubRestClient restClient,
                         GitHubProperties properties) {
        this.client = client;
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<RepositorioMetrica> coletar() {
        List<RepositorioMetrica> resultado = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        Instant agora = Instant.now();

        int total = properties.getTotalRepos();
        int pageSize = properties.getPageSize();

        log.info("Iniciando coleta de {} repositórios...", total);

        while (resultado.size() < total && hasNextPage) {
            int restante = total - resultado.size();
            int qtd = Math.min(pageSize, restante);

            log.info("Buscando página com {} repositórios (coletados até agora: {})", qtd, resultado.size());

            SearchResult pagina = client.buscarPagina(QUERY_STRING, qtd, cursor);

            for (RepositoryNode node : pagina.nodes()) {
                long idadeEmMeses = ChronoUnit.MONTHS.between(
                        node.createdAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        agora.atZone(ZoneOffset.UTC).toLocalDate());

                // Em dias, não em meses: meses truncados zeram quase todos os repositórios
                // populares, que costumam ter push recente. pushedAt é null em repo sem commits.
                long diasDesdeUltimoPush = node.pushedAt() == null
                        ? RepositorioMetrica.SEM_DATA_DE_PUSH
                        : ChronoUnit.DAYS.between(node.pushedAt(), agora);

                int totalReleases = node.releases() == null ? 0 : node.releases().totalCount();

                // Conexoes aninhadas podem faltar: linguagem e nula em repositorio que nao e
                // codigo, e as contagens somem quando o recurso esta desabilitado.
                String linguagem = node.primaryLanguage() == null ? "" : node.primaryLanguage().name();
                int issuesAbertas = node.issuesAbertas() == null ? 0 : node.issuesAbertas().totalCount();
                int issuesFechadas = node.issuesFechadas() == null ? 0 : node.issuesFechadas().totalCount();
                int prsAceitas = node.prsAceitas() == null ? 0 : node.prsAceitas().totalCount();

                resultado.add(new RepositorioMetrica(
                        node.nameWithOwner(),
                        node.stargazerCount(),
                        idadeEmMeses,
                        totalReleases,
                        node.pushedAt(),
                        node.updatedAt(),
                        diasDesdeUltimoPush,
                        linguagem,
                        issuesAbertas,
                        issuesFechadas,
                        prsAceitas));
            }

            hasNextPage = pagina.pageInfo().hasNextPage();
            cursor = pagina.pageInfo().endCursor();
        }

        log.info("Coleta finalizada: {} repositórios.", resultado.size());
        return corrigirReleasesNoTeto(resultado);
    }

    /**
     * Reconta pela API REST os repositórios cujo total de releases veio no teto do GraphQL.
     * Sem isso, a métrica da RQ03 fica censurada à direita e a média sai subestimada.
     */
    private List<RepositorioMetrica> corrigirReleasesNoTeto(List<RepositorioMetrica> repositorios) {
        List<RepositorioMetrica> corrigidos = new ArrayList<>(repositorios.size());
        int quantidade = 0;

        for (RepositorioMetrica repositorio : repositorios) {
            if (repositorio.totalReleases() < TETO_RELEASES_GRAPHQL) {
                corrigidos.add(repositorio);
                continue;
            }

            int real = restClient.contarReleases(repositorio.nome(), repositorio.totalReleases());
            log.info("Releases no teto — {}: {} (GraphQL) -> {} (REST)",
                    repositorio.nome(), repositorio.totalReleases(), real);
            corrigidos.add(repositorio.comTotalReleases(real));
            quantidade++;
        }

        if (quantidade > 0) {
            log.info("{} repositório(s) tiveram o total de releases corrigido via REST.", quantidade);
        }

        return corrigidos;
    }
}