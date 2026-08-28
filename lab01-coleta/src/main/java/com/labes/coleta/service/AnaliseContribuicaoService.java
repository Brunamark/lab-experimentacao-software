package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contribuição externa recebida, medida em pull requests aceitos (RQ02).
 *
 * <p>Média e mediana andam juntas aqui pelo mesmo motivo da RQB03: a distribuição tem cauda
 * longa — na amostra vão de dezenas a dezenas de milhares — e a média sozinha é deslocada por
 * poucos repositórios muito ativos.
 */
@Service
public class AnaliseContribuicaoService {

    public double mediaPrsAceitos(List<RepositorioMetrica> repositorios) {
        if (repositorios.isEmpty()) {
            return 0;
        }
        return arredondar(repositorios.stream()
                .mapToInt(RepositorioMetrica::prsAceitas)
                .average()
                .orElse(0));
    }

    public double medianaPrsAceitos(List<RepositorioMetrica> repositorios) {
        List<Integer> valores = repositorios.stream()
                .map(RepositorioMetrica::prsAceitas)
                .sorted()
                .toList();

        if (valores.isEmpty()) {
            return 0;
        }
        int meio = valores.size() / 2;
        double valor = valores.size() % 2 == 1
                ? valores.get(meio)
                : (valores.get(meio - 1) + valores.get(meio)) / 2.0;
        return arredondar(valor);
    }

    public long totalPrsAceitos(List<RepositorioMetrica> repositorios) {
        return repositorios.stream().mapToLong(RepositorioMetrica::prsAceitas).sum();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
