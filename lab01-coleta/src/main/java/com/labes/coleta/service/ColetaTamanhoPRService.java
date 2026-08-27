package com.labes.coleta.service;

import com.labes.coleta.client.GitHubGraphQLClient;
import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.PullRequestSizeNode;
import com.labes.coleta.dto.PullRequestSizeResult;
import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualTamanhoPR;
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
 * Coleta, por ano, o tamanho médio (additions + deletions) dos PRs aceitos numa amostra
 * de repositórios. Para não estourar o custo da API, amostra apenas os N primeiros PRs
 * aceitos de cada repositório/ano (ordenados por data de criação).
 */
@Service
public class ColetaTamanhoPRService {

    private static final Logger log = LoggerFactory.getLogger(ColetaTamanhoPRService.class);
    /** Menor que no ColetaPullRequestsService: cada alias traz até N nós, não uma contagem. */
    private static final int COMBINACOES_POR_LOTE = 10;
    /**
     * Pausa entre lotes bem-sucedidos. Sem isso, uma coleta de 1000 repositórios dispara
     * centenas de requisições sequenciais sem folga, o que aumenta a chance de estourar o
     * rate limit secundário do GitHub e receber 502/504 (esgotando as tentativas do client).
     */
    private static final long PAUSA_ENTRE_LOTES_MS = 500;

    private final GitHubGraphQLClient client;
    private final GitHubProperties properties;

    public ColetaTamanhoPRService(GitHubGraphQLClient client, GitHubProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<TendenciaAnualTamanhoPR> coletar(List<RepositorioMetrica> repositorios) {
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
        log.info("Analisando tamanho de PRs de {} repositórios em {} anos ({} combinações, {} requisições)...",
                nomes.size(), anos.size(), combinacoes.size(), totalLotes);

        Map<Integer, long[]> totaisPorAno = new HashMap<>(); // ano -> [soma de linhas, qtd de PRs]

        for (int inicio = 0; inicio < combinacoes.size(); inicio += COMBINACOES_POR_LOTE) {
            List<Combinacao> lote = combinacoes.subList(inicio, Math.min(inicio + COMBINACOES_POR_LOTE, combinacoes.size()));
            Map<String, PullRequestSizeResult> resultado;
            try {
                resultado = client.buscarTamanhosEmLote(montarQuery(lote, anoAtual));
            } catch (RuntimeException e) {
                // Mesmo depois do retry do client, um lote pode continuar batendo 502 (ex.:
                // repositório caro de indexar naquele ano). Pular esse lote em vez de abortar
                // a coleta inteira preserva as centenas de combinações já processadas.
                log.warn("Lote {}-{} falhou mesmo após retries — pulando ({} combinações perdidas).",
                        inicio, inicio + lote.size() - 1, lote.size(), e);
                continue;
            }

            for (int i = 0; i < lote.size(); i++) {
                Combinacao c = lote.get(i);
                PullRequestSizeResult prs = resultado.get("c" + i);

                if (prs == null || prs.nodes() == null) {
                    continue;
                }

                long[] totais = totaisPorAno.computeIfAbsent(c.ano(), a -> new long[2]);
                for (PullRequestSizeNode no : prs.nodes()) {
                    // Nós sem additions/deletions vêm de itens que não são PullRequest — ignorados.
                    if (no == null || no.additions() == null || no.deletions() == null) {
                        continue;
                    }
                    totais[0] += no.additions() + no.deletions();
                    totais[1]++;
                }
            }

            log.info("Lote de tamanho de PRs processado: {}/{} combinações",
                    Math.min(inicio + lote.size(), combinacoes.size()), combinacoes.size());

            if (inicio + COMBINACOES_POR_LOTE < combinacoes.size()) {
                aguardar(PAUSA_ENTRE_LOTES_MS);
            }
        }

        return anos.stream()
                .map(ano -> {
                    long[] totais = totaisPorAno.getOrDefault(ano, new long[2]);
                    double media = totais[1] == 0 ? 0 : Math.round(totais[0] * 100.0 / totais[1]) / 100.0;
                    return new TendenciaAnualTamanhoPR(ano, media, (int) totais[1]);
                })
                .toList();
    }

    private String montarQuery(List<Combinacao> lote, int anoAtual) {
        StringBuilder sb = new StringBuilder("query {\n");
        int amostra = properties.getAmostraPrsTamanho();

        for (int i = 0; i < lote.size(); i++) {
            Combinacao c = lote.get(i);
            LocalDate inicio = LocalDate.of(c.ano(), 1, 1);
            LocalDate fim = c.ano() == anoAtual ? LocalDate.now() : LocalDate.of(c.ano(), 12, 31);

            sb.append("  c").append(i).append(": search(query: \"repo:").append(c.repositorio())
                    .append(" is:pr is:merged merged:").append(inicio).append("..").append(fim)
                    .append(" sort:created-asc\", type: ISSUE, first: ").append(amostra).append(") {\n")
                    .append("    nodes { ... on PullRequest { additions deletions } }\n")
                    .append("  }\n");
        }

        return sb.append("}").toString();
    }

    private void aguardar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Coleta interrompida durante a pausa entre lotes.", e);
        }
    }

    private record Combinacao(String repositorio, int ano) {
    }
}
