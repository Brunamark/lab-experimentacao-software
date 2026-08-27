package com.labes.coleta.service;

import com.labes.coleta.client.GitHubGraphQLClient;
import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.dto.IssueCommitsNode;
import com.labes.coleta.dto.IssueCommitsResult;
import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualCommitsPorIssue;
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
 * Coleta, por ano, quantos commits em média foram necessários para fechar uma issue (RQB03).
 *
 * <p>Cada combinação repositório × ano gera dois aliases: um conta todas as issues fechadas no
 * ano e o outro amostra as issues fechadas <em>com Pull Request vinculado</em>
 * ({@code linked:pr}), somando os commits desses PRs.
 *
 * <p>O filtro {@code linked:pr} não é detalhe de performance, é o que torna a métrica válida.
 * Sem ele, medido contra a API real, apenas 1 a 4 de cada 20 issues amostradas tinham
 * fechamento rastreável — e a amostra era dominada por faxina de backlog fechada por bot, o
 * que inflava a média em até 7×. Com ele, a atribuição fica em 10 de 10 e a média passa a
 * descrever a população sobre a qual a pergunta faz sentido.
 *
 * <p>Como a amostra é enviesada por construção (só issues resolvidas por PR), as contagens de
 * universo viajam junto no resultado: são elas que dizem que fatia das issues fechadas a média
 * representa e se essa fatia mudou ao longo dos anos.
 */
@Service
public class ColetaCommitsPorIssueService {

    private static final Logger log = LoggerFactory.getLogger(ColetaCommitsPorIssueService.class);

    /** Medido contra a API real: ~8,7s por lote de 10, sem 502. */
    private static final int COMBINACOES_POR_LOTE = 10;

    /**
     * Pausa entre lotes bem-sucedidos. A API de busca tem limite secundário próprio e uma
     * coleta de 1000 repositórios são ~500 requisições sequenciais: sem folga, o GitHub
     * começa a devolver 403/502. Subir para 2000 ms se aparecer "secondary rate limit".
     */
    private static final long PAUSA_ENTRE_LOTES_MS = 500;

    /** Teto de PRs lidos por issue. Issues resolvidas por mais de 10 PRs são raras. */
    private static final int MAX_PRS_POR_ISSUE = 10;

    private final GitHubGraphQLClient client;
    private final GitHubProperties properties;

    public ColetaCommitsPorIssueService(GitHubGraphQLClient client, GitHubProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<TendenciaAnualCommitsPorIssue> coletar(List<RepositorioMetrica> repositorios) {
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
        log.info("Analisando commits por issue de {} repositórios em {} anos ({} combinações, {} requisições)...",
                nomes.size(), anos.size(), combinacoes.size(), totalLotes);

        // Guardamos o valor de cada issue, e não só a soma, porque a mediana exige a amostra
        // inteira. São ~50 mil inteiros na coleta dos 1000 repositórios — irrelevante em memória.
        Map<Integer, List<Integer>> amostraPorAno = new HashMap<>();
        // ano -> [issues com PR no universo, issues fechadas no universo]
        Map<Integer, long[]> universoPorAno = new HashMap<>();
        int lotesPulados = 0;

        for (int inicio = 0; inicio < combinacoes.size(); inicio += COMBINACOES_POR_LOTE) {
            List<Combinacao> lote = combinacoes.subList(inicio,
                    Math.min(inicio + COMBINACOES_POR_LOTE, combinacoes.size()));

            Map<String, IssueCommitsResult> resultado;
            try {
                resultado = client.buscarCommitsPorIssueEmLote(montarQuery(lote, anoAtual));
            } catch (RuntimeException e) {
                // Mesma política do ColetaTamanhoPRService: um lote que continua falhando
                // depois dos retries é pulado, para não perder as centenas de combinações já
                // processadas numa coleta que dura mais de uma hora.
                log.warn("Lote {}-{} falhou mesmo após retries — pulando ({} combinações perdidas).",
                        inicio, inicio + lote.size() - 1, lote.size(), e);
                lotesPulados++;
                continue;
            }

            for (int i = 0; i < lote.size(); i++) {
                Combinacao c = lote.get(i);
                long[] universo = universoPorAno.computeIfAbsent(c.ano(), a -> new long[2]);

                IssueCommitsResult fechadas = resultado.get("t" + i);
                if (fechadas != null) {
                    universo[1] += fechadas.issueCount();
                }

                IssueCommitsResult comPr = resultado.get("c" + i);
                if (comPr == null) {
                    continue;
                }
                universo[0] += comPr.issueCount();

                if (comPr.nodes() == null) {
                    continue;
                }
                List<Integer> amostra = amostraPorAno.computeIfAbsent(c.ano(), a -> new ArrayList<>());
                for (IssueCommitsNode issue : comPr.nodes()) {
                    int commits = commitsDosPrsQueFecharam(issue);
                    if (commits > 0) {
                        amostra.add(commits);
                    }
                }
            }

            log.info("Lote de commits por issue processado: {}/{} combinações",
                    Math.min(inicio + lote.size(), combinacoes.size()), combinacoes.size());

            if (inicio + COMBINACOES_POR_LOTE < combinacoes.size()) {
                aguardar(PAUSA_ENTRE_LOTES_MS);
            }
        }

        if (lotesPulados > 0) {
            log.warn("{} lote(s) de commits por issue foram perdidos — registrar no relatório.", lotesPulados);
        }

        return anos.stream()
                .map(ano -> {
                    List<Integer> amostra = amostraPorAno.getOrDefault(ano, List.of());
                    long[] universo = universoPorAno.getOrDefault(ano, new long[2]);
                    return new TendenciaAnualCommitsPorIssue(
                            ano, media(amostra), mediana(amostra), amostra.size(),
                            universo[0], universo[1]);
                })
                .toList();
    }

    private double media(List<Integer> amostra) {
        if (amostra.isEmpty()) {
            return 0;
        }
        long soma = 0;
        for (int commits : amostra) {
            soma += commits;
        }
        return arredondar(soma / (double) amostra.size());
    }

    /**
     * Mediana da amostra. Vai ao lado da média porque a distribuição de commits por PR tem
     * cauda longa: no piloto, uma issue isolada dobrou a média de um ano inteiro.
     */
    private double mediana(List<Integer> amostra) {
        if (amostra.isEmpty()) {
            return 0;
        }
        List<Integer> ordenada = new ArrayList<>(amostra);
        ordenada.sort(null);

        int meio = ordenada.size() / 2;
        double valor = ordenada.size() % 2 == 1
                ? ordenada.get(meio)
                : (ordenada.get(meio - 1) + ordenada.get(meio)) / 2.0;

        return arredondar(valor);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Soma dos commits de todos os PRs que fecharam a issue. Issues resolvidas por mais de um
     * PR contam o total: o que se quer medir é o esforço para resolver a issue, não o tamanho
     * de um PR isolado.
     */
    private int commitsDosPrsQueFecharam(IssueCommitsNode issue) {
        if (issue == null
                || issue.closedByPullRequestsReferences() == null
                || issue.closedByPullRequestsReferences().nodes() == null) {
            return 0;
        }

        int total = 0;
        for (IssueCommitsNode.PullRequest pr : issue.closedByPullRequestsReferences().nodes()) {
            if (pr != null && pr.commits() != null) {
                total += pr.commits().totalCount();
            }
        }
        return total;
    }

    private String montarQuery(List<Combinacao> lote, int anoAtual) {
        StringBuilder sb = new StringBuilder("query {\n");
        int amostra = properties.getAmostraIssuesCommits();

        for (int i = 0; i < lote.size(); i++) {
            Combinacao c = lote.get(i);
            LocalDate inicio = LocalDate.of(c.ano(), 1, 1);
            LocalDate fim = c.ano() == anoAtual ? LocalDate.now() : LocalDate.of(c.ano(), 12, 31);
            String periodo = " is:issue is:closed closed:" + inicio + ".." + fim;

            // Universo: todas as issues fechadas no ano (contagem, sem trazer nós).
            sb.append("  t").append(i).append(": search(query: \"repo:").append(c.repositorio())
                    .append(periodo).append("\", type: ISSUE, first: 1) { issueCount }\n");

            // Amostra: issues fechadas com PR vinculado, com os commits desses PRs.
            sb.append("  c").append(i).append(": search(query: \"repo:").append(c.repositorio())
                    .append(periodo).append(" linked:pr\", type: ISSUE, first: ").append(amostra).append(") {\n")
                    .append("    issueCount\n")
                    .append("    nodes { ... on Issue {\n")
                    .append("      closedByPullRequestsReferences(first: ").append(MAX_PRS_POR_ISSUE)
                    .append(", includeClosedPrs: true) {\n")
                    .append("        totalCount\n")
                    .append("        nodes { commits { totalCount } }\n")
                    .append("      }\n")
                    .append("    } }\n")
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
