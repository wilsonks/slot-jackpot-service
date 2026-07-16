package com.slotcentral.jackpot.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateJackpotRequest(
    String name,
    @DecimalMin("0.01") BigDecimal baseAmount,
    @DecimalMin("0.000001") BigDecimal incrementRate,
    Boolean isActive
) {}
