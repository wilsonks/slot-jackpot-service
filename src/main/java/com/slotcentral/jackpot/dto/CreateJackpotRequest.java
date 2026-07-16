package com.slotcentral.jackpot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateJackpotRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.01") BigDecimal baseAmount,
    @NotNull @DecimalMin("0.000001") BigDecimal incrementRate
) {}
