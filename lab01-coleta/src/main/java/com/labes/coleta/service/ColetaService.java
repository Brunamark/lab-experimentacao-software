package com.labes.coleta.service;

import com.labes.coleta.client.GitHubGraphQLClient;
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

    private final GitHubGraphQLClient client;
    private final GitHubProperties properties;

    public ColetaService(GitHubGraphQLClient client, GitHubProperties properties) {
        this.client = client;
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
                long mesesDesdeUltimoPush = ChronoUnit.MONTHS.between(
                        node.pushedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        agora.atZone(ZoneOffset.UTC).toLocalDate());
                resultado.add(new RepositorioMetrica(
                        node.nameWithOwner(), node.stargazerCount(), idadeEmMeses, mesesDesdeUltimoPush));
            }

            hasNextPage = pagina.pageInfo().hasNextPage();
            cursor = pagina.pageInfo().endCursor();
        }

        log.info("Coleta finalizada: {} repositórios.", resultado.size());
        return resultado;
    }
}