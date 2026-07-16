package com.slotcentral.jackpot.dto;

import com.slotcentral.jackpot.domain.JackpotWinHistory;
import java.math.BigDecimal;
import java.time.Instant;

public record WinHistoryResponse(
    Long id,
    Long jackpotId,
    String wonBy,
    BigDecimal wonAmount,
    String egmId,
    Instant wonAt,
    String spinId
) {
    public static WinHistoryResponse from(JackpotWinHistory h) {
        return new WinHistoryResponse(
            h.getId(), h.getJackpotId(), h.getWonBy(), h.getWonAmount(),
            h.getEgmId(), h.getWonAt(), h.getSpinId()
        );
    }
}
