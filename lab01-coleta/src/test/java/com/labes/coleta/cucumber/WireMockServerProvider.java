package com.labes.coleta.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Profile("mock")
@TestComponent
public class WireMockServerProvider {

    private final WireMockServer wireMockServer;

    public WireMockServerProvider() {
        wireMockServer = new WireMockServer(options().port(8089));
    }

    public WireMockServer get() {
        return wireMockServer;
    }

    public void start() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }

    public void resetScenarios() {
        wireMockServer.resetScenarios();
    }
}
