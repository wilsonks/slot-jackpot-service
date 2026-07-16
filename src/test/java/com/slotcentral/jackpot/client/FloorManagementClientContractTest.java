package com.slotcentral.jackpot.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FloorManagementClientContractTest {

    private static WireMockServer wireMock;
    private FloorManagementClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        client = new FloorManagementClient(RestClient.builder(), "http://localhost:" + wireMock.port());
    }

    @Test
    void broadcastRollup_sendsCorrectRequest() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/floor/jackpot-broadcast/rollup"))
            .willReturn(aResponse().withStatus(200)));

        assertDoesNotThrow(() -> client.broadcastRollup(1L, new BigDecimal("1500.50"), "EGM-001"));

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/floor/jackpot-broadcast/rollup"))
            .withRequestBody(matchingJsonPath("$.jackpotId"))
            .withRequestBody(matchingJsonPath("$.newAmount"))
            .withRequestBody(matchingJsonPath("$.sourceEgmId")));
    }

    @Test
    void broadcastWin_sendsCorrectRequest() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/floor/jackpot-broadcast/win"))
            .willReturn(aResponse().withStatus(200)));

        assertDoesNotThrow(() -> client.broadcastWin(1L, new BigDecimal("5000.00"), "EGM-002", "player-1"));

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/floor/jackpot-broadcast/win"))
            .withRequestBody(matchingJsonPath("$.jackpotId"))
            .withRequestBody(matchingJsonPath("$.wonAmount"))
            .withRequestBody(matchingJsonPath("$.egmId"))
            .withRequestBody(matchingJsonPath("$.wonBy")));
    }

    @Test
    void broadcastReset_sendsCorrectRequest() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/floor/jackpot-broadcast/reset"))
            .willReturn(aResponse().withStatus(200)));

        assertDoesNotThrow(() -> client.broadcastReset(1L, new BigDecimal("1000.00")));

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/floor/jackpot-broadcast/reset")));
    }

    @Test
    void broadcastResetToEgm_sendsCorrectRequest() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/floor/jackpot-broadcast/reset/EGM-005"))
            .willReturn(aResponse().withStatus(200)));

        assertDoesNotThrow(() -> client.broadcastResetToEgm(1L, new BigDecimal("1000.00"), "EGM-005"));

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v1/floor/jackpot-broadcast/reset/EGM-005")));
    }

    @Test
    void broadcastRollup_doesNotThrowOnFloorManagementUnavailable() {
        FloorManagementClient clientWithBadUrl = new FloorManagementClient(
            RestClient.builder(), "http://localhost:19999"
        );
        assertDoesNotThrow(() -> clientWithBadUrl.broadcastRollup(1L, new BigDecimal("100.00"), "EGM-1"));
    }

    @Test
    void broadcastWin_doesNotThrowOnFloorManagementUnavailable() {
        FloorManagementClient clientWithBadUrl = new FloorManagementClient(
            RestClient.builder(), "http://localhost:19999"
        );
        assertDoesNotThrow(() -> clientWithBadUrl.broadcastWin(1L, new BigDecimal("5000.00"), "EGM-1", "player-1"));
    }
}
