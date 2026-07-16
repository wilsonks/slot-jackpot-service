package com.slotcentral.jackpot.dto;

import java.math.BigDecimal;

public record ContributeResponse(
    JackpotResponse jackpot,
    BigDecimal contributionAmount,
    String spinId,
    boolean idempotentReplay
) {}
