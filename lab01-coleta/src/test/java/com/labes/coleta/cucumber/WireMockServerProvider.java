package com.labes.coleta.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;

@Profile("mock")
@TestComponent
public class WireMockServerProvider {

    private final WireMockServer wireMockServer;

    public WireMockServerProvider() {
        wireMockServer = new WireMockServer();
    }

    public WireMockServer get() {
        return wireMockServer;
    }

    public void resetScenarios() {
        wireMockServer.resetScenarios();
    }
}
