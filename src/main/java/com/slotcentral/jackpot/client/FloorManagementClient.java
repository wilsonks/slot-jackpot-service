package com.slotcentral.jackpot.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class FloorManagementClient {

    private static final Logger log = LoggerFactory.getLogger(FloorManagementClient.class);

    private final RestClient restClient;

    public FloorManagementClient(
            RestClient.Builder builder,
            @Value("${floor-management.base-url:http://localhost:8086}") String baseUrl) {
        this.restClient = builder
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(baseUrl)
            .build();
    }

    public void broadcastRollup(Long jackpotId, BigDecimal newAmount, String sourceEgmId) {
        try {
            restClient.post()
                .uri("/api/v1/floor/jackpot-broadcast/rollup")
                .body(Map.of(
                    "jackpotId", jackpotId,
                    "newAmount", newAmount,
                    "sourceEgmId", sourceEgmId != null ? sourceEgmId : ""
                ))
                .retrieve()
                .toBodilessEntity();
            log.debug("Floor broadcast rollup sent for jackpot {}, amount={}", jackpotId, newAmount);
        } catch (RestClientException ex) {
            log.warn("Floor broadcast rollup failed for jackpot {} (non-fatal): {}", jackpotId, ex.getMessage());
        }
    }

    public void broadcastWin(Long jackpotId, BigDecimal wonAmount, String egmId, String wonBy) {
        try {
            restClient.post()
                .uri("/api/v1/floor/jackpot-broadcast/win")
                .body(Map.of(
                    "jackpotId", jackpotId,
                    "wonAmount", wonAmount,
                    "egmId", egmId != null ? egmId : "",
                    "wonBy", wonBy
                ))
                .retrieve()
                .toBodilessEntity();
            log.debug("Floor broadcast win sent for jackpot {}, egmId={}", jackpotId, egmId);
        } catch (RestClientException ex) {
            log.warn("Floor broadcast win failed for jackpot {} (non-fatal): {}", jackpotId, ex.getMessage());
        }
    }

    public void broadcastReset(Long jackpotId, BigDecimal baseAmount) {
        try {
            restClient.post()
                .uri("/api/v1/floor/jackpot-broadcast/reset")
                .body(Map.of(
                    "jackpotId", jackpotId,
                    "baseAmount", baseAmount
                ))
                .retrieve()
                .toBodilessEntity();
            log.debug("Floor broadcast reset sent for jackpot {}", jackpotId);
        } catch (RestClientException ex) {
            log.warn("Floor broadcast reset failed for jackpot {} (non-fatal): {}", jackpotId, ex.getMessage());
        }
    }

    public void broadcastResetToEgm(Long jackpotId, BigDecimal baseAmount, String egmId) {
        try {
            restClient.post()
                .uri("/api/v1/floor/jackpot-broadcast/reset/{egmId}", egmId)
                .body(Map.of(
                    "jackpotId", jackpotId,
                    "baseAmount", baseAmount
                ))
                .retrieve()
                .toBodilessEntity();
            log.debug("Floor broadcast reset sent for jackpot {} to egm {}", jackpotId, egmId);
        } catch (RestClientException ex) {
            log.warn("Floor broadcast reset/egm failed for jackpot {} (non-fatal): {}", jackpotId, ex.getMessage());
        }
    }
}
