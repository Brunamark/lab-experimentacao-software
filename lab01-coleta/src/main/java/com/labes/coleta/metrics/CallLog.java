package com.labes.coleta.metrics;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registra, em memória, cada requisição HTTP feita pelos clients (GraphQL e REST)
 * durante um cenário de teste. É a fonte de dados para as métricas de idempotência,
 * chamadas redundantes, cobertura de paginação e fallback.
 *
 * <p>É reiniciado a cada cenário (ver {@link com.labes.coleta.cucumber.Hooks}), então
 * cada instância só enxerga as chamadas do cenário corrente.
 */
@Component
public class CallLog {

    /**
     * @param assinatura identifica uma requisição "logicamente igual" (mesmo método + URL +
     *                    corpo). Duas chamadas com a mesma assinatura são, do ponto de vista
     *                    da API, o mesmo pedido repetido.
     */
    public record Chamada(Instant timestamp, String metodo, String url, String assinatura,
                           int statusCode, boolean falhou) {
    }

    private final List<Chamada> chamadas = Collections.synchronizedList(new ArrayList<>());

    public void reset() {
        chamadas.clear();
    }

    public void registrar(String metodo, String url, String assinatura, int statusCode, boolean falhou) {
        chamadas.add(new Chamada(Instant.now(), metodo, url, assinatura, statusCode, falhou));
    }

    public List<Chamada> todas() {
        return List.copyOf(chamadas);
    }

    public int totalChamadas() {
        return chamadas.size();
    }

    public long chamadasUnicas() {
        return chamadas.stream().map(Chamada::assinatura).distinct().count();
    }

    /**
     * Uma chamada é "redundante" quando repete a assinatura de uma chamada anterior que
     * já tinha tido SUCESSO. Repetir uma chamada que falhou não conta aqui — isso é
     * retry legítimo, ver {@link #tentativasDeRetry()}.
     */
    public long chamadasRedundantes() {
        Map<String, Long> porAssinatura = snapshot().stream()
                .map(Chamada::assinatura)
                .filter(a -> a != null && !a.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return porAssinatura.values().stream()
                .filter(v -> v > 1)
                .mapToLong(v -> v - 1)
                .sum();
    }

    /**
     * Uma chamada conta como "retry" quando repete a assinatura de uma chamada
     * IMEDIATAMENTE anterior que falhou — o padrão esperado do laço de retry do
     * GitHubGraphQLClient.
     */
    public long tentativasDeRetry() {
        Map<String, List<Chamada>> porAssinatura = snapshot().stream()
                .filter(c -> c.assinatura() != null && !c.assinatura().isBlank())
                .collect(Collectors.groupingBy(Chamada::assinatura));

        return porAssinatura.values().stream()
                .filter(lista -> lista.size() > 1)
                .filter(lista -> lista.stream().anyMatch(Chamada::falhou))
                .count();
    }

    public long chamadasPara(String trechoUrl) {
        return snapshot().stream()
                .filter(c -> c.url() != null && c.url().contains(trechoUrl))
                .count();
    }

    public long chamadasUnicasPara(String trechoUrl) {
        return snapshot().stream()
                .filter(c -> c.url() != null && c.url().contains(trechoUrl))
                .map(Chamada::assinatura)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .count();
    }

    private List<Chamada> snapshot() {
        synchronized (chamadas) {
            return new ArrayList<>(chamadas);
        }
    }
}
