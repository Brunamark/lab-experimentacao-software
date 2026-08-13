package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Gera arquivos .csv a partir dos dados coletados, sem bibliotecas de terceiros. */
@Component
public class CsvExportService {

    private static final String CABECALHO = "nome,estrelas,idade_meses,meses_desde_ultimo_push";
    private static final String CABECALHO_TENDENCIA_PRS = "ano,prs_criadas,prs_aceitas,taxa_aceitacao";

    public void exportar(List<RepositorioMetrica> repositorios, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO).append('\n');

        for (RepositorioMetrica r : repositorios) {
            sb.append('"').append(r.nome()).append("\",")
                    .append(r.estrelas()).append(',')
                    .append(r.idadeEmMeses()).append(',')
                    .append(r.mesesDesdeUltimoPush())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }

    public void exportarTendenciaPRs(List<TendenciaAnualPullRequests> tendencia, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO_TENDENCIA_PRS).append('\n');

        for (TendenciaAnualPullRequests t : tendencia) {
            sb.append(t.ano()).append(',')
                    .append(t.prsCriadas()).append(',')
                    .append(t.prsAceitas()).append(',')
                    .append(t.taxaAceitacao())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }
}