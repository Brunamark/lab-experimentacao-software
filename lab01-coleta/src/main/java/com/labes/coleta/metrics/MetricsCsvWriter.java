package com.labes.coleta.metrics;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Escreve métricas em formato longo (uma linha por métrica) em
 * target/cucumber-metrics/metricas.csv. Formato longo facilita a leitura em pandas
 * e permite adicionar métricas novas sem precisar mudar o cabeçalho do CSV.
 */
@Component
public class MetricsCsvWriter {

    private static final Path ARQUIVO = Path.of("target", "cucumber-metrics", "metricas.csv");

    public synchronized void escrever(String feature, String cenario, String metrica, Object valor) {
        try {
            Files.createDirectories(ARQUIVO.getParent());
            boolean novo = !Files.exists(ARQUIVO);

            try (BufferedWriter writer = Files.newBufferedWriter(ARQUIVO,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (novo) {
                    writer.write("feature;cenario;metrica;valor");
                    writer.newLine();
                }
                writer.write("%s;%s;%s;%s".formatted(
                        limpar(feature), limpar(cenario), limpar(metrica), String.valueOf(valor)));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao escrever métrica '%s' do cenário '%s'"
                    .formatted(metrica, cenario), e);
        }
    }

    /** Remove ';' e quebras de linha para não corromper o CSV (delimitador simples, sem aspas). */
    private String limpar(String valor) {
        return valor.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}
