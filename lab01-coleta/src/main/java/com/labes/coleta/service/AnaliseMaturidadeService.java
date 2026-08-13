package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcula estatísticas de idade sobre os repositórios coletados (RQ01).
 *
 * <p>As métricas de tempo desde o último push ficam em {@link AnaliseAtualizacaoService} (RQ04).
 */
@Service
public class AnaliseMaturidadeService {

    private static final int TOP_N = 15;

    /** Tempo médio, em meses, desde a criação dos repositórios — quanto maior, mais "velhos" em média. */
    public double idadeMediaEmMeses(List<RepositorioMetrica> repositorios) {
        var media = repositorios.stream()
                .mapToLong(RepositorioMetrica::idadeEmMeses)
                .average()
                .orElse(0);
        return Math.round(media * 100.0) / 100.0;
    }

    /** Mesma idade média, mas só entre os 15 repositórios mais populares (mais estrelas). */
    public double idadeMediaTop15(List<RepositorioMetrica> repositorios) {
        return idadeMediaEmMeses(repositorios.stream().limit(TOP_N).toList());
    }

    /** Mesma idade média, mas só entre os 15 repositórios menos populares dos top 100 obtidos (mais estrelas). */
    public double idadeMediaLast15(List<RepositorioMetrica> repositorios) {
        return idadeMediaEmMeses(repositorios.reversed().stream().limit(TOP_N).toList());
    }
}
