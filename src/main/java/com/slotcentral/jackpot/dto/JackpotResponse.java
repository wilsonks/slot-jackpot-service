package com.slotcentral.jackpot.dto;

import com.slotcentral.jackpot.domain.Jackpot;
import java.math.BigDecimal;
import java.time.Instant;

public record JackpotResponse(
    Long id,
    String name,
    BigDecimal baseAmount,
    BigDecimal incrementRate,
    BigDecimal currentAmount,
    boolean isActive,
    String wonBy,
    BigDecimal wonAmount,
    Instant wonAt,
    Instant lastIncrementedAt,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
    public static JackpotResponse from(Jackpot j) {
        return new JackpotResponse(
            j.getId(), j.getName(), j.getBaseAmount(), j.getIncrementRate(),
            j.getCurrentAmount(), j.isActive(), j.getWonBy(), j.getWonAmount(),
            j.getWonAt(), j.getLastIncrementedAt(), j.getCreatedAt(), j.getUpdatedAt(),
            j.getVersion()
        );
    }
}
