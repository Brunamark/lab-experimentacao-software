package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Gera o arquivo .csv a partir da lista de repositórios coletados, sem bibliotecas de terceiros. */
@Component
public class CsvExportService {

    private static final String CABECALHO = "nome,estrelas";

    public void exportar(List<RepositorioMetrica> repositorios, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO).append('\n');

        for (RepositorioMetrica r : repositorios) {
            sb.append('"').append(r.nome()).append("\",")
                    .append(r.estrelas())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }
}