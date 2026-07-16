package com.slotcentral.jackpot.dto;

public record WinResponse(
    JackpotResponse jackpot,
    WinHistoryResponse winRecord,
    boolean idempotentReplay
) {}
