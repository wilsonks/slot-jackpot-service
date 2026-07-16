package com.slotcentral.jackpot.integration;

import com.slotcentral.jackpot.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestSecurityConfig.class)
class JackpotIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    private Long jackpotId;

    @BeforeEach
    void createJackpot() {
        CreateJackpotRequest req = new CreateJackpotRequest(
            "Test Jackpot", new BigDecimal("500.00"), new BigDecimal("0.005"));
        ResponseEntity<JackpotResponse> resp = restTemplate.postForEntity(
            "/api/v1/jackpots", req, JackpotResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        jackpotId = resp.getBody().id();
    }

    @Test
    void crud_createAndGetJackpot() {
        ResponseEntity<JackpotResponse> resp = restTemplate.getForEntity(
            "/api/v1/jackpots/" + jackpotId, JackpotResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().name()).isEqualTo("Test Jackpot");
        assertThat(resp.getBody().currentAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void contribute_incrementsCurrentAmount() {
        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), "EGM-1", "spin-001");
        ResponseEntity<ContributeResponse> resp = restTemplate.postForEntity(
            "/api/v1/jackpots/" + jackpotId + "/contribute", req, ContributeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().jackpot().currentAmount()).isEqualByComparingTo(new BigDecimal("500.50"));
        assertThat(resp.getBody().contributionAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
    }

    @Test
    void contribute_idempotentOnDuplicateSpinId() {
        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), "EGM-1", "spin-idem-001");
        restTemplate.postForEntity("/api/v1/jackpots/" + jackpotId + "/contribute", req, ContributeResponse.class);

        ResponseEntity<ContributeResponse> replay = restTemplate.postForEntity(
            "/api/v1/jackpots/" + jackpotId + "/contribute", req, ContributeResponse.class);
        assertThat(replay.getBody().idempotentReplay()).isTrue();
    }

    @Test
    void win_recordsWinAndResetsAmount() {
        WinRequest req = new WinRequest("player-99", "EGM-2", "spin-win-001");
        ResponseEntity<WinResponse> resp = restTemplate.postForEntity(
            "/api/v1/jackpots/" + jackpotId + "/win", req, WinResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().winRecord().wonAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(resp.getBody().jackpot().currentAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        ResponseEntity<Map> histResp = restTemplate.getForEntity(
            "/api/v1/jackpots/" + jackpotId + "/wins", Map.class);
        assertThat(histResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void win_idempotentOnDuplicateSpinId() {
        WinRequest req = new WinRequest("player-99", "EGM-2", "spin-win-idem-001");
        restTemplate.postForEntity("/api/v1/jackpots/" + jackpotId + "/win", req, WinResponse.class);

        ResponseEntity<WinResponse> replay = restTemplate.postForEntity(
            "/api/v1/jackpots/" + jackpotId + "/win", req, WinResponse.class);
        assertThat(replay.getBody().idempotentReplay()).isTrue();
    }

    @Test
    void winHistory_preservedAcrossMultipleWins() {
        WinRequest win1 = new WinRequest("player-1", "EGM-1", "spin-hist-001");
        WinRequest win2 = new WinRequest("player-2", "EGM-2", "spin-hist-002");

        restTemplate.postForEntity("/api/v1/jackpots/" + jackpotId + "/win", win1, WinResponse.class);
        restTemplate.postForEntity("/api/v1/jackpots/" + jackpotId + "/contribute",
            new ContributeRequest(new BigDecimal("1000.00"), null, "spin-contrib-hist"), ContributeResponse.class);
        restTemplate.postForEntity("/api/v1/jackpots/" + jackpotId + "/win", win2, WinResponse.class);

        ResponseEntity<Map> histResp = restTemplate.getForEntity(
            "/api/v1/jackpots/" + jackpotId + "/wins?size=20", Map.class);
        Map body = histResp.getBody();
        assertThat(((java.util.List<?>) body.get("content")).size()).isEqualTo(2);
    }
}
