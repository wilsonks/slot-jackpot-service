package com.slotcentral.jackpot.service;

import com.slotcentral.jackpot.client.FloorManagementClient;
import com.slotcentral.jackpot.domain.Jackpot;
import com.slotcentral.jackpot.domain.JackpotContribution;
import com.slotcentral.jackpot.domain.JackpotWinHistory;
import com.slotcentral.jackpot.dto.*;
import com.slotcentral.jackpot.repository.JackpotContributionRepository;
import com.slotcentral.jackpot.repository.JackpotRepository;
import com.slotcentral.jackpot.repository.JackpotWinHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JackpotTransactionalOpsTest {

    @Mock JackpotRepository jackpotRepo;
    @Mock JackpotWinHistoryRepository winHistoryRepo;
    @Mock JackpotContributionRepository contributionRepo;
    @Mock FloorManagementClient floorClient;

    @InjectMocks JackpotTransactionalOps ops;

    private Jackpot jackpot;

    @BeforeEach
    void setUp() {
        jackpot = new Jackpot();
        jackpot.setId(1L);
        jackpot.setName("Grand Jackpot");
        jackpot.setBaseAmount(new BigDecimal("1000.00"));
        jackpot.setIncrementRate(new BigDecimal("0.005"));
        jackpot.setCurrentAmount(new BigDecimal("1500.00"));
        jackpot.setActive(true);
        jackpot.setVersion(0L);
    }

    @Test
    void executeContribute_computesCorrectContributionAmount() {
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));
        when(jackpotRepo.save(any())).thenReturn(jackpot);

        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), "EGM-1", "spin-001");
        ContributeResponse response = ops.executeContribute(1L, req);

        assertThat(response.contributionAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(response.idempotentReplay()).isFalse();
        verify(contributionRepo).save(any(JackpotContribution.class));
    }

    @Test
    void executeContribute_withNoSpinId_doesNotSaveContribution() {
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));
        when(jackpotRepo.save(any())).thenReturn(jackpot);

        ContributeRequest req = new ContributeRequest(new BigDecimal("50.00"), null, null);
        ops.executeContribute(1L, req);

        verify(contributionRepo, never()).save(any());
    }

    @Test
    void executeWin_setsWonFieldsAndResetsCurrentAmount() {
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));
        when(jackpotRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(winHistoryRepo.save(any())).thenAnswer(inv -> {
            JackpotWinHistory h = inv.getArgument(0);
            h.setId(99L);
            return h;
        });

        WinRequest req = new WinRequest("player-42", "EGM-1", "spin-win-001");
        WinResponse response = ops.executeWin(1L, req);

        assertThat(response.idempotentReplay()).isFalse();
        assertThat(response.winRecord().wonAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.winRecord().wonBy()).isEqualTo("player-42");
        assertThat(response.jackpot().currentAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(winHistoryRepo).save(any(JackpotWinHistory.class));
        verify(floorClient).broadcastWin(eq(1L), any(), eq("EGM-1"), eq("player-42"));
    }
}
