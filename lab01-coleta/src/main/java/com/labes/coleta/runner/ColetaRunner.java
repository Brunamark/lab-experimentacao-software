package com.labes.coleta.runner;

import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.service.AnaliseAtualizacaoService;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import com.labes.coleta.service.AnaliseMaturidadeService;
import com.labes.coleta.service.AnaliseReleasesService;
import com.labes.coleta.service.ColetaPullRequestsService;
import com.labes.coleta.service.ColetaService;
import com.labes.coleta.service.CsvExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ponto de entrada da execução: roda automaticamente quando a aplicação Spring Boot sobe,
 * coleta os dados e salva o CSV. Como é um script (não uma API), a aplicação é
 * configurada como não-web (application.yml) e encerra sozinha ao final do run().
 */
@Component
public class ColetaRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ColetaRunner.class);
    private static final String ARQUIVO_SAIDA = "repositorios_top100.csv";
    private static final String ARQUIVO_SAIDA_TENDENCIA_PRS = "tendencia_prs.csv";

    private final ColetaService coletaService;
    private final CsvExportService csvExportService;
    private final AnaliseMaturidadeService analiseMaturidadeService;
    private final AnaliseReleasesService analiseReleasesService;
    private final AnaliseAtualizacaoService analiseAtualizacaoService;
    private final ColetaPullRequestsService coletaPullRequestsService;

    public ColetaRunner(ColetaService coletaService, CsvExportService csvExportService,
                         AnaliseReleasesService analiseReleasesService,
                         AnaliseAtualizacaoService analiseAtualizacaoService,
                         AnaliseMaturidadeService analiseMaturidadeService,
                         ColetaPullRequestsService coletaPullRequestsService) {
        this.coletaService = coletaService;
        this.csvExportService = csvExportService;
        this.analiseMaturidadeService = analiseMaturidadeService;
        this.analiseReleasesService = analiseReleasesService;
        this.analiseAtualizacaoService = analiseAtualizacaoService;
        this.coletaPullRequestsService = coletaPullRequestsService;
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

        var semDataDePush = analiseAtualizacaoService.quantidadeSemDataDePush(repositorios);
        if (semDataDePush > 0) {
            log.warn("{} repositório(s) sem data de push foram descartados das estatísticas.",
                    semDataDePush);
        }

        List<TendenciaAnualPullRequests> tendenciaPRs = coletaPullRequestsService.coletar(repositorios);
        csvExportService.exportarTendenciaPRs(tendenciaPRs, ARQUIVO_SAIDA_TENDENCIA_PRS);
        log.info("Tendência de PRs salva em: {}", ARQUIVO_SAIDA_TENDENCIA_PRS);
        for (TendenciaAnualPullRequests t : tendenciaPRs) {
            log.info("Ano {}: {} PRs criados, {} aceitos (taxa de aceitação: {}%)",
                    t.ano(), t.prsCriadas(), t.prsAceitas(), t.taxaAceitacao());
        }
    }
}
