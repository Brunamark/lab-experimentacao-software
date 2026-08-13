package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcula estatísticas sobre o total de releases: sistemas populares lançam
 * releases com frequência?
 */
@Service
public class AnaliseReleasesService {

    private static final int TOP_N = 15;
    private static final double MESES_POR_ANO = 12.0;

    /** Total médio de releases — métrica principal da RQ03. */
    public double mediaReleases(List<RepositorioMetrica> repositorios) {
        var media = repositorios.stream()
                .mapToInt(RepositorioMetrica::totalReleases)
                .average()
                .orElse(0);
        return arredondar(media);
    }

    /** Mesma média, mas só entre os 15 repositórios mais populares. */
    public double mediaReleasesTop15(List<RepositorioMetrica> repositorios) {
        return mediaReleases(repositorios.stream().limit(TOP_N).toList());
    }

    /**
     * Quantos repositórios nunca publicaram uma release — muitos projetos versionam só por tag,
     * e boa parte do topo do ranking são listas curadas, sem release nenhuma.
     */
    public long quantidadeSemRelease(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .filter(r -> r.totalReleases() == 0)
                .count();
    }

    /** Percentual de repositórios sem nenhuma release, para acompanhar a média na discussão. */
    public double percentualSemRelease(List<RepositorioMetrica> repositorios) {
        if (repositorios.isEmpty()) {
            return 0;
        }
        return arredondar(100.0 * quantidadeSemRelease(repositorios) / repositorios.size());
    }

    /**
     * Releases por ano do conjunto: soma das releases dividida pela soma dos anos de vida.
     *
     * <p>"Com frequência" é uma taxa, e o total absoluto favorece repositórios antigos. Mas a
     * taxa precisa ser <b>agregada</b>, e não a média das taxas individuais: dividir releases
     * por uma idade pequena infla a taxa de um repositório jovem sem limite, e um único caso
     * desses domina a média. A forma agregada pondera cada repositório pelo tempo que ele
     * realmente viveu.
     *
     * <p>Repositórios com menos de um mês de vida ficam de fora dos dois somatórios, para não
     * entrar no numerador sem contribuir para o denominador.
     */
    public double releasesPorAno(List<RepositorioMetrica> repositorios) {
        var comIdade = repositorios.stream()
                .filter(r -> r.idadeEmMeses() > 0)
                .toList();

        double totalReleases = comIdade.stream()
                .mapToInt(RepositorioMetrica::totalReleases)
                .sum();
        double totalAnos = comIdade.stream()
                .mapToDouble(r -> r.idadeEmMeses() / MESES_POR_ANO)
                .sum();

        return totalAnos == 0 ? 0 : arredondar(totalReleases / totalAnos);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
