package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Percentual de issues fechadas (RQ06).
 *
 * <p>Repositórios sem nenhuma issue (recurso desabilitado) são descartados: a razão seria uma
 * divisão por zero, e contá-los como 0% confundiria "nenhuma issue fechada" com "nenhuma issue".
 */
@Service
public class AnaliseIssuesService {

    private static final double LIMIAR_ALTO = 90.0;

    /**
     * Razão agregada — soma das fechadas dividida pela soma dos totais. É a resposta principal
     * da RQ06.
     *
     * <p>Não usar a média dos percentuais individuais: um repositório com 3 issues, todas
     * fechadas, contribuiria com 100% e pesaria igual a um com 22.000 issues. É a mesma
     * distorção registrada no achado #3 de achados-e-correcoes-coleta.md.
     */
    public double percentualFechadasAgregado(List<RepositorioMetrica> repositorios) {
        long fechadas = 0;
        long totais = 0;
        for (RepositorioMetrica r : repositorios) {
            if (r.temIssues()) {
                fechadas += r.issuesFechadas();
                totais += r.issuesTotais();
            }
        }
        return totais == 0 ? 0 : arredondar(100.0 * fechadas / totais);
    }

    /** Mediana dos percentuais individuais, para contrastar com a razão agregada. */
    public double percentualFechadasMediana(List<RepositorioMetrica> repositorios) {
        List<Double> percentuais = repositorios.stream()
                .filter(RepositorioMetrica::temIssues)
                .map(r -> 100.0 * r.issuesFechadas() / r.issuesTotais())
                .sorted()
                .toList();

        if (percentuais.isEmpty()) {
            return 0;
        }
        int meio = percentuais.size() / 2;
        double valor = percentuais.size() % 2 == 1
                ? percentuais.get(meio)
                : (percentuais.get(meio - 1) + percentuais.get(meio)) / 2.0;
        return arredondar(valor);
    }

    /** Quantos repositórios têm ao menos 90% das issues fechadas. */
    public long quantidadeAcimaDe90(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .filter(RepositorioMetrica::temIssues)
                .filter(r -> 100.0 * r.issuesFechadas() / r.issuesTotais() >= LIMIAR_ALTO)
                .count();
    }

    /** Repositórios com issues desabilitadas, descartados das estatísticas acima. */
    public long quantidadeSemIssues(List<RepositorioMetrica> repositorios) {
        return repositorios.stream().filter(r -> !r.temIssues()).count();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
