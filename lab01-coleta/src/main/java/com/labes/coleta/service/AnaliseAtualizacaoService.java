package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula estatísticas sobre o tempo desde a última atualização (RQ04): sistemas populares
 * são atualizados com frequência?
 *
 * <p>A métrica primária é o {@code pushedAt} (último push de código), e não o {@code updatedAt},
 * porque este último também muda por alterações de metadados (descrição, topics), o que infla
 * artificialmente a sensação de atividade.
 *
 * <p>Repositórios sem data de push são descartados de todos os cálculos.
 */
@Service
public class AnaliseAtualizacaoService {

    private static final int TOP_N = 15;
    private static final int DIAS_RECENTE = 30;

    /** Tempo médio, em dias, desde o último push — métrica principal da RQ04. */
    public double mediaDiasDesdeUltimoPush(List<RepositorioMetrica> repositorios) {
        var media = comDataDePush(repositorios).stream()
                .mapToLong(RepositorioMetrica::diasDesdeUltimoPush)
                .average()
                .orElse(0);
        return arredondar(media);
    }

    /** Mesma média, mas só entre os 15 repositórios mais populares. */
    public double mediaDiasDesdeUltimoPushTop15(List<RepositorioMetrica> repositorios) {
        return mediaDiasDesdeUltimoPush(repositorios.stream().limit(TOP_N).toList());
    }

    /**
     * Percentual atualizado nos últimos 30 dias. Como a média é puxada para cima pelos
     * repositórios abandonados, é este número que mostra o quanto a maioria está viva.
     */
    public double percentualAtualizadosUltimos30Dias(List<RepositorioMetrica> repositorios) {
        var validos = comDataDePush(repositorios);
        if (validos.isEmpty()) {
            return 0;
        }
        long recentes = validos.stream()
                .filter(r -> r.diasDesdeUltimoPush() <= DIAS_RECENTE)
                .count();
        return arredondar(100.0 * recentes / validos.size());
    }

    /**
     * Distribuição por faixa de recência. Conta a história melhor que a média sozinha e é a
     * base do gráfico da RQ04.
     */
    public Map<String, Long> distribuicaoPorFaixa(List<RepositorioMetrica> repositorios) {
        Map<String, Long> faixas = new LinkedHashMap<>();
        faixas.put("ate 7 dias", contarEntre(repositorios, 0, 7));
        faixas.put("8 a 30 dias", contarEntre(repositorios, 8, 30));
        faixas.put("31 a 90 dias", contarEntre(repositorios, 31, 90));
        faixas.put("91 a 365 dias", contarEntre(repositorios, 91, 365));
        faixas.put("mais de 365 dias", contarEntre(repositorios, 366, Long.MAX_VALUE));
        return faixas;
    }

    /** Quantos repositórios vieram sem data de push e ficaram de fora das estatísticas. */
    public long quantidadeSemDataDePush(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .filter(r -> !r.temDataDePush())
                .count();
    }

    /** Conta os repositórios com dias no intervalo fechado [minimo, maximo]. */
    private long contarEntre(List<RepositorioMetrica> repositorios, long minimo, long maximo) {
        return comDataDePush(repositorios).stream()
                .filter(r -> r.diasDesdeUltimoPush() >= minimo
                        && r.diasDesdeUltimoPush() <= maximo)
                .count();
    }

    private List<RepositorioMetrica> comDataDePush(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .filter(RepositorioMetrica::temDataDePush)
                .toList();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
