package com.cargo.booking.client;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.cargo.booking.BaseIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class BaseWireMockTest extends BaseIntegrationTest {

    private static final WireMockServer SCHEDULE_API = startWireMockServer();
    private static final WireMockServer EQUIPMENT_API = startWireMockServer();
    private static final WireMockServer QUOTE_API = startWireMockServer();

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("app.integration.schedule-api.base-url", SCHEDULE_API::baseUrl);
        registry.add("app.integration.equipment-api.base-url", EQUIPMENT_API::baseUrl);
        registry.add("app.integration.quote-api.base-url", QUOTE_API::baseUrl);
    }

    @BeforeEach
    void resetWireMockServers() {
        SCHEDULE_API.resetAll();
        EQUIPMENT_API.resetAll();
        QUOTE_API.resetAll();
    }

    protected WireMockServer scheduleApi() {
        return SCHEDULE_API;
    }

    protected WireMockServer equipmentApi() {
        return EQUIPMENT_API;
    }

    protected WireMockServer quoteApi() {
        return QUOTE_API;
    }

    private static WireMockServer startWireMockServer() {
        WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        return server;
    }
}
