package com.labes.coleta.runner;

import com.labes.coleta.model.RepositorioMetrica;
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

    private final ColetaService coletaService;
    private final CsvExportService csvExportService;

    public ColetaRunner(ColetaService coletaService, CsvExportService csvExportService) {
        this.coletaService = coletaService;
        this.csvExportService = csvExportService;
    }

    @Override
    public void run(String... args) throws Exception {
        List<RepositorioMetrica> repositorios = coletaService.coletar();
        csvExportService.exportar(repositorios, ARQUIVO_SAIDA);
        log.info("Dados salvos em: {}", ARQUIVO_SAIDA);
    }
}
