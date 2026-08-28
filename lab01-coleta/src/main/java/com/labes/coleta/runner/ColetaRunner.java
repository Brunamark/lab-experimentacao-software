package com.labes.coleta.runner;

import com.labes.coleta.config.GitHubProperties;
import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.service.AnaliseAtualizacaoService;
import com.labes.coleta.model.TendenciaAnualCommitsPorIssue;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import com.labes.coleta.model.TendenciaAnualTamanhoPR;
import com.labes.coleta.service.AnaliseContribuicaoService;
import com.labes.coleta.service.AnaliseIssuesService;
import com.labes.coleta.service.AnaliseLinguagemService;
import com.labes.coleta.service.AnaliseMaturidadeService;
import com.labes.coleta.service.AnaliseReleasesService;
import com.labes.coleta.service.ColetaCommitsPorIssueService;
import com.labes.coleta.service.ColetaPullRequestsService;
import com.labes.coleta.service.ColetaService;
import com.labes.coleta.service.ColetaTamanhoPRService;
import com.labes.coleta.service.CsvExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ponto de entrada da execução: roda automaticamente quando a aplicação Spring Boot sobe,
 * coleta os dados e salva o CSV. Como é um script (não uma API), a aplicação é
 * configurada como não-web (application.yml) e encerra sozinha ao final do run().
 */
@Component
@Profile("!mock & !test & !realtest")
public class ColetaRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ColetaRunner.class);
    private static final String ARQUIVO_SAIDA = "repositorios_top100.csv";
    private static final String ARQUIVO_SAIDA_TENDENCIA_PRS = "tendencia_prs.csv";
    private static final String ARQUIVO_SAIDA_TAMANHO_PRS = "tendencia_tamanho_prs.csv";
    private static final String ARQUIVO_SAIDA_COMMITS_POR_ISSUE = "commits_por_issue.csv";

    /** Quantas linguagens entram na tabela de distribuição da RQ05. */
    private static final int TOP_LINGUAGENS = 12;

    /**
     * Mínimo de repositórios para uma linguagem entrar no recorte da RQ07. Sem esse piso, uma
     * linguagem com dois repositórios apareceria na tabela com a mesma autoridade que uma com
     * cento e cinquenta.
     */
    private static final int PISO_RQ07 = 10;

    private final ColetaService coletaService;
    private final CsvExportService csvExportService;
    private final AnaliseMaturidadeService analiseMaturidadeService;
    private final AnaliseReleasesService analiseReleasesService;
    private final AnaliseAtualizacaoService analiseAtualizacaoService;
    private final ColetaPullRequestsService coletaPullRequestsService;
    private final ColetaTamanhoPRService coletaTamanhoPRService;
    private final ColetaCommitsPorIssueService coletaCommitsPorIssueService;
    private final AnaliseLinguagemService analiseLinguagemService;
    private final AnaliseIssuesService analiseIssuesService;
    private final AnaliseContribuicaoService analiseContribuicaoService;
    private final GitHubProperties properties;

    public ColetaRunner(ColetaService coletaService, CsvExportService csvExportService,
                         AnaliseReleasesService analiseReleasesService,
                         AnaliseAtualizacaoService analiseAtualizacaoService,
                         AnaliseMaturidadeService analiseMaturidadeService,
                         ColetaPullRequestsService coletaPullRequestsService,
                         ColetaTamanhoPRService coletaTamanhoPRService,
                         ColetaCommitsPorIssueService coletaCommitsPorIssueService,
                         AnaliseLinguagemService analiseLinguagemService,
                         AnaliseIssuesService analiseIssuesService,
                         AnaliseContribuicaoService analiseContribuicaoService,
                         GitHubProperties properties) {
        this.coletaService = coletaService;
        this.csvExportService = csvExportService;
        this.analiseMaturidadeService = analiseMaturidadeService;
        this.analiseReleasesService = analiseReleasesService;
        this.analiseAtualizacaoService = analiseAtualizacaoService;
        this.coletaPullRequestsService = coletaPullRequestsService;
        this.coletaTamanhoPRService = coletaTamanhoPRService;
        this.coletaCommitsPorIssueService = coletaCommitsPorIssueService;
        this.analiseLinguagemService = analiseLinguagemService;
        this.analiseIssuesService = analiseIssuesService;
        this.analiseContribuicaoService = analiseContribuicaoService;
        this.properties = properties;
    }

    @Override
    public void run(String... args) throws Exception {
        List<RepositorioMetrica> repositorios = coletaService.coletar();
        csvExportService.exportar(repositorios, ARQUIVO_SAIDA);
        log.info("Dados salvos em: {}", ARQUIVO_SAIDA);

        var idadeMedia = analiseMaturidadeService.idadeMediaEmMeses(repositorios);
        var idadeMediaTop15 = analiseMaturidadeService.idadeMediaTop15(repositorios);
        var idadeMediaLast15 = analiseMaturidadeService.idadeMediaLast15(repositorios);
        log.info("Idade média: {} meses | Idade média (top 15): {} meses | Idade média (last 15): {} meses",
                idadeMedia,
                idadeMediaTop15,
                idadeMediaLast15);

        log.info("Releases — média: {} | (top 15): {} | taxa agregada: {} releases/ano",
                analiseReleasesService.mediaReleases(repositorios),
                analiseReleasesService.mediaReleasesTop15(repositorios),
                analiseReleasesService.releasesPorAno(repositorios));
        log.info("Repositórios sem nenhuma release: {} ({}%)",
                analiseReleasesService.quantidadeSemRelease(repositorios),
                analiseReleasesService.percentualSemRelease(repositorios));

        log.info("Dias desde o último push — média: {} | (top 15): {} | atualizados nos últimos 30 dias: {}%",
                analiseAtualizacaoService.mediaDiasDesdeUltimoPush(repositorios),
                analiseAtualizacaoService.mediaDiasDesdeUltimoPushTop15(repositorios),
                analiseAtualizacaoService.percentualAtualizadosUltimos30Dias(repositorios));
        log.info("Distribuição por faixa de recência: {}",
                analiseAtualizacaoService.distribuicaoPorFaixa(repositorios));

        log.info("RQ02 | PRs aceitos — média: {} | mediana: {} | total na amostra: {}",
                analiseContribuicaoService.mediaPrsAceitos(repositorios),
                analiseContribuicaoService.medianaPrsAceitos(repositorios),
                analiseContribuicaoService.totalPrsAceitos(repositorios));

        log.info("RQ05 | Linguagens mais frequentes: {}",
                analiseLinguagemService.topLinguagens(repositorios, TOP_LINGUAGENS));
        log.info("RQ05 | Sem linguagem primária (repositórios de conteúdo): {} ({}%)",
                analiseLinguagemService.quantidadeSemLinguagem(repositorios),
                analiseLinguagemService.percentualSemLinguagem(repositorios));

        log.info("RQ06 | Issues fechadas — razão agregada: {}% | mediana dos percentuais: {}%",
                analiseIssuesService.percentualFechadasAgregado(repositorios),
                analiseIssuesService.percentualFechadasMediana(repositorios));
        log.info("RQ06 | Repositórios com ao menos 90% fechadas: {} | sem nenhuma issue: {}",
                analiseIssuesService.quantidadeAcimaDe90(repositorios),
                analiseIssuesService.quantidadeSemIssues(repositorios));

        // RQ07: as mesmas métricas das RQ02/RQ03/RQ04, agora por linguagem. Nenhum service novo
        // é necessário porque todos recebem a lista por parâmetro. Os métodos *Top15 ficam de
        // fora: eles pressupõem a lista ordenada por estrelas, o que não vale numa sublista.
        var porLinguagem = analiseLinguagemService.agruparPorLinguagem(repositorios, PISO_RQ07);
        log.info("RQ07 | {} linguagens com ao menos {} repositórios:", porLinguagem.size(), PISO_RQ07);
        porLinguagem.forEach((linguagem, repos) ->
                log.info("RQ07 | {} ({} repos) — PRs aceitos (mediana): {} | releases (média): {} | dias desde o push (média): {}",
                        linguagem, repos.size(),
                        analiseContribuicaoService.medianaPrsAceitos(repos),
                        analiseReleasesService.mediaReleases(repos),
                        analiseAtualizacaoService.mediaDiasDesdeUltimoPush(repos)));

        var semDataDePush = analiseAtualizacaoService.quantidadeSemDataDePush(repositorios);
        if (semDataDePush > 0) {
            log.warn("{} repositório(s) sem data de push foram descartados das estatísticas.",
                    semDataDePush);
        }

        // As coletas de tendência são caras (horas, nos 1000 repositórios) e independentes
        // entre si. As flags permitem rodar uma métrica isolada por variável de ambiente,
        // sem comentar código aqui — o default continua sendo a coleta completa.
        if (properties.isColetarTendenciaPrs()) {
            List<TendenciaAnualPullRequests> tendenciaPRs = coletaPullRequestsService.coletar(repositorios);
            csvExportService.exportarTendenciaPRs(tendenciaPRs, ARQUIVO_SAIDA_TENDENCIA_PRS);
            log.info("Tendência de PRs salva em: {}", ARQUIVO_SAIDA_TENDENCIA_PRS);
            for (TendenciaAnualPullRequests t : tendenciaPRs) {
                log.info("Ano {}: {} PRs criados, {} aceitos (taxa de aceitação: {}%)",
                        t.ano(), t.prsCriadas(), t.prsAceitas(), t.taxaAceitacao());
            }
        } else {
            log.info("Coleta de tendência de PRs desativada (coletar-tendencia-prs=false).");
        }

        if (properties.isColetarTamanhoPrs()) {
            List<TendenciaAnualTamanhoPR> tendenciaTamanho = coletaTamanhoPRService.coletar(repositorios);
            csvExportService.exportarTendenciaTamanhoPRs(tendenciaTamanho, ARQUIVO_SAIDA_TAMANHO_PRS);
            log.info("Tendência de tamanho de PRs salva em: {}", ARQUIVO_SAIDA_TAMANHO_PRS);
            for (TendenciaAnualTamanhoPR t : tendenciaTamanho) {
                log.info("Ano {}: tamanho médio {} linhas (amostra: {} PRs)",
                        t.ano(), t.tamanhoMedioLinhas(), t.amostraPRs());
            }
        } else {
            log.info("Coleta de tamanho de PRs desativada (coletar-tamanho-prs=false).");
        }

        if (properties.isColetarCommitsPorIssue()) {
            List<TendenciaAnualCommitsPorIssue> commitsPorIssue =
                    coletaCommitsPorIssueService.coletar(repositorios);
            csvExportService.exportarCommitsPorIssue(commitsPorIssue, ARQUIVO_SAIDA_COMMITS_POR_ISSUE);
            log.info("Commits por issue salvos em: {}", ARQUIVO_SAIDA_COMMITS_POR_ISSUE);
            for (TendenciaAnualCommitsPorIssue t : commitsPorIssue) {
                double fatiaComPr = t.issuesFechadasNoUniverso() == 0 ? 0
                        : Math.round(t.issuesComPrNoUniverso() * 10000.0 / t.issuesFechadasNoUniverso()) / 100.0;
                log.info("Ano {}: media {} | mediana {} commits por issue (amostra: {} issues | {} de {} issues fechadas tinham PR: {}%)",
                        t.ano(), t.commitsMediosPorIssue(), t.commitsMedianosPorIssue(),
                        t.issuesAnalisadas(), t.issuesComPrNoUniverso(),
                        t.issuesFechadasNoUniverso(), fatiaComPr);
            }
        } else {
            log.info("Coleta de commits por issue desativada (coletar-commits-por-issue=false).");
        }
    }
}
