package com.labes.coleta.cucumber;

import com.labes.coleta.model.RepositorioMetrica;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ScenarioScope
public class ContextoCenario {

    int totalDisponivel;
    int pageSize;
    String nameWithOwner;
    String nomeCenario;
    String nomeFeature;

    /** true quando um step "Dado" já registrou os stubs manualmente (fallback/falha transitória). */
    boolean stubConfiguradoManualmente;

    List<RepositorioMetrica> resultado1;
    List<RepositorioMetrica> resultado2;
    RuntimeException excecaoLancada;

    void reset() {
        totalDisponivel = 0;
        pageSize = 0;
        nameWithOwner = null;
        nomeCenario = null;
        nomeFeature = null;
        stubConfiguradoManualmente = false;
        resultado1 = new ArrayList<>();
        resultado2 = new ArrayList<>();
        excecaoLancada = null;
    }
}
