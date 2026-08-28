package com.labes.coleta.service;

import com.labes.coleta.model.RepositorioMetrica;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Distribuição de linguagens primárias na amostra (RQ05) e agrupamento que sustenta a RQ07.
 *
 * <p>Repositórios sem linguagem primária não são erro de coleta: são os de conteúdo (listas
 * curadas, livros, roadmaps). Quantos deles existem é, em si, um resultado da RQ05, então eles
 * entram na distribuição sob um rótulo próprio em vez de serem descartados.
 */
@Service
public class AnaliseLinguagemService {

    /** Linguagem -> nº de repositórios, da mais frequente para a menos. */
    public Map<String, Long> distribuicao(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .collect(Collectors.groupingBy(RepositorioMetrica::linguagemOuRotulo, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** As N linguagens mais frequentes, para a tabela do relatório. */
    public Map<String, Long> topLinguagens(List<RepositorioMetrica> repositorios, int n) {
        return distribuicao(repositorios).entrySet().stream()
                .limit(n)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Quantos repositórios não têm linguagem primária — os de conteúdo. */
    public long quantidadeSemLinguagem(List<RepositorioMetrica> repositorios) {
        return repositorios.stream()
                .filter(r -> RepositorioMetrica.SEM_LINGUAGEM.equals(r.linguagemOuRotulo()))
                .count();
    }

    public double percentualSemLinguagem(List<RepositorioMetrica> repositorios) {
        if (repositorios.isEmpty()) {
            return 0;
        }
        return arredondar(100.0 * quantidadeSemLinguagem(repositorios) / repositorios.size());
    }

    /**
     * Agrupa por linguagem, mantendo apenas as que têm ao menos {@code piso} repositórios.
     *
     * <p>O piso não é detalhe de apresentação: sem ele, uma linguagem com dois repositórios
     * aparece na tabela da RQ07 com a mesma autoridade que uma com cento e cinquenta.
     */
    public Map<String, List<RepositorioMetrica>> agruparPorLinguagem(
            List<RepositorioMetrica> repositorios, int piso) {
        return repositorios.stream()
                .collect(Collectors.groupingBy(RepositorioMetrica::linguagemOuRotulo))
                .entrySet().stream()
                .filter(e -> e.getValue().size() >= piso)
                .sorted(Comparator.comparingInt((Map.Entry<String, List<RepositorioMetrica>> e)
                        -> e.getValue().size()).reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
