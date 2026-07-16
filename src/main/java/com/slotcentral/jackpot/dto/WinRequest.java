package com.slotcentral.jackpot.dto;

import jakarta.validation.constraints.NotBlank;

public record WinRequest(
    @NotBlank String uid,
    String egmId,
    String spinId
) {}
