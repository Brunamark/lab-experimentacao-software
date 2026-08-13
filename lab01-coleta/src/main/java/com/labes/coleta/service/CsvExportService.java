package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Gera o arquivo .csv a partir da lista de repositórios coletados, sem bibliotecas de terceiros. */
@Component
public class CsvExportService {

    private static final String CABECALHO =
            "nome,estrelas,idade_meses,total_releases,ultimo_push,ultima_atualizacao,dias_desde_ultimo_push";

    public void exportar(List<RepositorioMetrica> repositorios, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO).append('\n');

        for (RepositorioMetrica r : repositorios) {
            sb.append('"').append(r.nome()).append("\",")
                    .append(r.estrelas()).append(',')
                    .append(r.idadeEmMeses()).append(',')
                    .append(r.totalReleases()).append(',')
                    .append(data(r.ultimoPush())).append(',')
                    .append(data(r.ultimaAtualizacao())).append(',')
                    .append(r.diasDesdeUltimoPush())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }

    /** Datas em ISO-8601 UTC, para o CSV continuar processável pelos scripts de análise. */
    private String data(Instant instante) {
        return instante == null ? "" : instante.toString();
    }
}