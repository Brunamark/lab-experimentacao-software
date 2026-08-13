package com.labes.coleta.runner;

import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import com.labes.coleta.service.AnaliseMaturidadeService;
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
    private final ColetaPullRequestsService coletaPullRequestsService;

    public ColetaRunner(ColetaService coletaService, CsvExportService csvExportService,
                         AnaliseMaturidadeService analiseMaturidadeService,
                         ColetaPullRequestsService coletaPullRequestsService) {
        this.coletaService = coletaService;
        this.csvExportService = csvExportService;
        this.analiseMaturidadeService = analiseMaturidadeService;
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

        var pushMedia = analiseMaturidadeService.mediaMesesDesdeUltimoPush(repositorios);
        var pushMediaTop15 = analiseMaturidadeService.mediaMesesDesdeUltimoPushTop15(repositorios);
        log.info("Meses desde último push (média): {} | (top 15): {}", pushMedia, pushMediaTop15);

        List<TendenciaAnualPullRequests> tendenciaPRs = coletaPullRequestsService.coletar(repositorios);
        csvExportService.exportarTendenciaPRs(tendenciaPRs, ARQUIVO_SAIDA_TENDENCIA_PRS);
        log.info("Tendência de PRs salva em: {}", ARQUIVO_SAIDA_TENDENCIA_PRS);
        for (TendenciaAnualPullRequests t : tendenciaPRs) {
            log.info("Ano {}: {} PRs criados, {} aceitos (taxa de aceitação: {}%)",
                    t.ano(), t.prsCriadas(), t.prsAceitas(), t.taxaAceitacao());
        }
    }
}
