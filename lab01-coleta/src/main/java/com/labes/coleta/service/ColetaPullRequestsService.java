package com.labes.coleta.service;

import com.labes.coleta.client.GitHubGraphQLClient;
import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.IssueCountResult;
import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Coleta, por ano, quantos PRs foram criados e aceitos (merged) numa amostra de
 * repositórios, via contagens em lote (aliases GraphQL).
 */
@Service
public class ColetaPullRequestsService {

    private static final Logger log = LoggerFactory.getLogger(ColetaPullRequestsService.class);
    private static final int COMBINACOES_POR_LOTE = 25; // 25 combinações = 50 aliases por requisição

    private final GitHubGraphQLClient client;
    private final GitHubProperties properties;

    public ColetaPullRequestsService(GitHubGraphQLClient client, GitHubProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<TendenciaAnualPullRequests> coletar(List<RepositorioMetrica> repositorios) {
        List<String> nomes = repositorios.stream()
                .limit(properties.getLimiteRepositoriosAnalisePrs())
                .map(RepositorioMetrica::nome)
                .toList();

        int anoAtual = LocalDate.now().getYear();
        List<Integer> anos = IntStream.rangeClosed(properties.getAnoInicioAnalise(), anoAtual).boxed().toList();

        List<Combinacao> combinacoes = new ArrayList<>();
        for (String nome : nomes) {
            for (int ano : anos) {
                combinacoes.add(new Combinacao(nome, ano));
            }
        }

        int totalLotes = (int) Math.ceil(combinacoes.size() / (double) COMBINACOES_POR_LOTE);
        log.info("Analisando PRs de {} repositórios em {} anos ({} combinações, {} requisições)...",
                nomes.size(), anos.size(), combinacoes.size(), totalLotes);

        Map<Integer, long[]> totaisPorAno = new HashMap<>(); // ano -> [criadas, aceitas]

        for (int inicio = 0; inicio < combinacoes.size(); inicio += COMBINACOES_POR_LOTE) {
            List<Combinacao> lote = combinacoes.subList(inicio, Math.min(inicio + COMBINACOES_POR_LOTE, combinacoes.size()));
            Map<String, IssueCountResult> resultado = client.buscarContagensEmLote(montarQuery(lote, anoAtual));

            for (int i = 0; i < lote.size(); i++) {
                Combinacao c = lote.get(i);
                long criadas = resultado.get("c" + i).issueCount();
                long aceitas = resultado.get("m" + i).issueCount();

                long[] totais = totaisPorAno.computeIfAbsent(c.ano(), a -> new long[2]);
                totais[0] += criadas;
                totais[1] += aceitas;
            }

            log.info("Lote de PRs processado: {}/{} combinações", Math.min(inicio + lote.size(), combinacoes.size()), combinacoes.size());
        }

        return anos.stream()
                .map(ano -> {
                    long[] totais = totaisPorAno.getOrDefault(ano, new long[2]);
                    double taxa = totais[0] == 0 ? 0 : Math.round(totais[1] * 10000.0 / totais[0]) / 100.0;
                    return new TendenciaAnualPullRequests(ano, totais[0], totais[1], taxa);
                })
                .toList();
    }

    private String montarQuery(List<Combinacao> lote, int anoAtual) {
        StringBuilder sb = new StringBuilder("query {\n");

        for (int i = 0; i < lote.size(); i++) {
            Combinacao c = lote.get(i);
            LocalDate inicio = LocalDate.of(c.ano(), 1, 1);
            LocalDate fim = c.ano() == anoAtual ? LocalDate.now() : LocalDate.of(c.ano(), 12, 31);

            sb.append("  c").append(i).append(": search(query: \"repo:").append(c.repositorio())
                    .append(" is:pr created:").append(inicio).append("..").append(fim)
                    .append("\", type: ISSUE, first: 1) { issueCount }\n");
            sb.append("  m").append(i).append(": search(query: \"repo:").append(c.repositorio())
                    .append(" is:pr is:merged merged:").append(inicio).append("..").append(fim)
                    .append("\", type: ISSUE, first: 1) { issueCount }\n");
        }

        return sb.append("}").toString();
    }

    private record Combinacao(String repositorio, int ano) {
    }
}
