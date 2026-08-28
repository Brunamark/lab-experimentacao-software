package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import com.labes.coleta.model.TendenciaAnualCommitsPorIssue;
import com.labes.coleta.model.TendenciaAnualPullRequests;
import com.labes.coleta.model.TendenciaAnualTamanhoPR;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Gera arquivos .csv a partir dos dados coletados, sem bibliotecas de terceiros. */
@Component
public class CsvExportService {

    private static final String CABECALHO =
            "nome,estrelas,idade_meses,total_releases,ultimo_push,ultima_atualizacao,"
                    + "dias_desde_ultimo_push,linguagem,issues_abertas,issues_fechadas,prs_aceitas";
    private static final String CABECALHO_TENDENCIA_PRS = "ano,prs_criadas,prs_aceitas,taxa_aceitacao";
    private static final String CABECALHO_TAMANHO_PRS = "ano,tamanho_medio_linhas,amostra_prs";
    private static final String CABECALHO_COMMITS_POR_ISSUE =
            "ano,commits_medios_por_issue,commits_medianos_por_issue,issues_analisadas,"
                    + "issues_com_pr_universo,issues_fechadas_universo";

    public void exportar(List<RepositorioMetrica> repositorios, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO).append('\n');

        for (RepositorioMetrica r : repositorios) {
            sb.append('"').append(r.nome()).append("\",")
                    .append(r.estrelas()).append(',')
                    .append(r.idadeEmMeses()).append(',')
                    .append(r.totalReleases()).append(',')
                    .append(data(r.ultimoPush())).append(',')
                    .append(data(r.ultimaAtualizacao())).append(',')
                    .append(r.diasDesdeUltimoPush()).append(',')
                    // Linguagem entre aspas: há nomes com espaço ("Jupyter Notebook", "Vim Script").
                    .append('"').append(r.linguagem()).append("\",")
                    .append(r.issuesAbertas()).append(',')
                    .append(r.issuesFechadas()).append(',')
                    .append(r.prsAceitas())
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

    public void exportarTendenciaTamanhoPRs(List<TendenciaAnualTamanhoPR> tendencia, String caminho) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO_TAMANHO_PRS).append('\n');

        for (TendenciaAnualTamanhoPR t : tendencia) {
            sb.append(t.ano()).append(',')
                    .append(t.tamanhoMedioLinhas()).append(',')
                    .append(t.amostraPRs())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Exporta a RQB03. As colunas de universo vão junto da média de propósito: a média cobre
     * só issues fechadas por PR, e sem saber que fatia do total isso representa em cada ano
     * não dá para distinguir mudança de comportamento de mudança na forma de fechar issues.
     */
    public void exportarCommitsPorIssue(List<TendenciaAnualCommitsPorIssue> tendencia, String caminho)
            throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO_COMMITS_POR_ISSUE).append('\n');

        for (TendenciaAnualCommitsPorIssue t : tendencia) {
            sb.append(t.ano()).append(',')
                    .append(t.commitsMediosPorIssue()).append(',')
                    .append(t.commitsMedianosPorIssue()).append(',')
                    .append(t.issuesAnalisadas()).append(',')
                    .append(t.issuesComPrNoUniverso()).append(',')
                    .append(t.issuesFechadasNoUniverso())
                    .append('\n');
        }

        Files.writeString(Path.of(caminho), sb.toString(), StandardCharsets.UTF_8);
    }

    /** Datas em ISO-8601 UTC, para o CSV continuar processável pelos scripts de análise. */
    private String data(Instant instante) {
        return instante == null ? "" : instante.toString();
    }
}