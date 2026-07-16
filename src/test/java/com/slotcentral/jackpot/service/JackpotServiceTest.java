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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JackpotServiceTest {

    @Mock JackpotRepository jackpotRepo;
    @Mock JackpotWinHistoryRepository winHistoryRepo;
    @Mock JackpotContributionRepository contributionRepo;
    @Mock FloorManagementClient floorClient;
    @Mock JackpotTransactionalOps transactionalOps;

    @InjectMocks JackpotService service;

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
    void contribute_idempotency_returnsPriorResultOnDuplicateSpinId() {
        JackpotContribution existing = new JackpotContribution();
        existing.setSpinId("spin-001");
        existing.setContributionAmount(new BigDecimal("0.50"));
        existing.setJackpotId(1L);

        when(contributionRepo.findBySpinId("spin-001")).thenReturn(Optional.of(existing));
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));

        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), "EGM-1", "spin-001");
        ContributeResponse response = service.contribute(1L, req);

        assertThat(response.idempotentReplay()).isTrue();
        assertThat(response.contributionAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        verify(transactionalOps, never()).executeContribute(any(), any());
    }

    @Test
    void contribute_callsTransactionalOpsWhenNoIdempotentMatch() {
        when(contributionRepo.findBySpinId("spin-001")).thenReturn(Optional.empty());
        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), "EGM-1", "spin-001");
        ContributeResponse expected = new ContributeResponse(
            JackpotResponse.from(jackpot), new BigDecimal("0.50"), "spin-001", false);
        when(transactionalOps.executeContribute(1L, req)).thenReturn(expected);

        ContributeResponse response = service.contribute(1L, req);

        assertThat(response.contributionAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(response.idempotentReplay()).isFalse();
        verify(transactionalOps).executeContribute(1L, req);
    }

    @Test
    void contribute_retriesOnOptimisticLockConflict() {
        when(contributionRepo.findBySpinId("spin-retry")).thenReturn(Optional.empty());
        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), null, "spin-retry");
        ContributeResponse expected = new ContributeResponse(
            JackpotResponse.from(jackpot), new BigDecimal("0.50"), "spin-retry", false);

        when(transactionalOps.executeContribute(eq(1L), eq(req)))
            .thenThrow(new ObjectOptimisticLockingFailureException("Jackpot", 1L))
            .thenThrow(new ObjectOptimisticLockingFailureException("Jackpot", 1L))
            .thenReturn(expected);

        ContributeResponse response = service.contribute(1L, req);

        assertThat(response).isNotNull();
        verify(transactionalOps, times(3)).executeContribute(1L, req);
    }

    @Test
    void contribute_throwsAfterExhaustingRetries() {
        when(contributionRepo.findBySpinId("spin-exhaust")).thenReturn(Optional.empty());
        ContributeRequest req = new ContributeRequest(new BigDecimal("100.00"), null, "spin-exhaust");

        when(transactionalOps.executeContribute(eq(1L), eq(req)))
            .thenThrow(new ObjectOptimisticLockingFailureException("Jackpot", 1L));

        assertThatThrownBy(() -> service.contribute(1L, req))
            .isInstanceOf(JackpotService.OptimisticLockRetryExhaustedException.class);

        verify(transactionalOps, times(6)).executeContribute(eq(1L), eq(req));
    }

    @Test
    void recordWin_idempotency_returnsPriorResultOnDuplicateSpinId() {
        JackpotWinHistory existingWin = new JackpotWinHistory();
        existingWin.setId(55L);
        existingWin.setJackpotId(1L);
        existingWin.setWonBy("player-42");
        existingWin.setWonAmount(new BigDecimal("1500.00"));
        existingWin.setSpinId("spin-win-001");
        existingWin.setWonAt(Instant.now());

        when(winHistoryRepo.findBySpinId("spin-win-001")).thenReturn(Optional.of(existingWin));
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));

        WinRequest req = new WinRequest("player-42", "EGM-1", "spin-win-001");
        WinResponse response = service.recordWin(1L, req);

        assertThat(response.idempotentReplay()).isTrue();
        verify(transactionalOps, never()).executeWin(any(), any());
    }

    @Test
    void adminReset_resetsCurrentAmountToBase() {
        when(jackpotRepo.findById(1L)).thenReturn(Optional.of(jackpot));
        when(jackpotRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JackpotResponse response = service.adminReset(1L);

        assertThat(response.currentAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(floorClient).broadcastReset(eq(1L), eq(new BigDecimal("1000.00")));
    }
}
