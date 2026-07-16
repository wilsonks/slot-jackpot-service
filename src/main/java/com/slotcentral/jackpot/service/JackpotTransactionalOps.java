package com.slotcentral.jackpot.service;

import com.slotcentral.jackpot.client.FloorManagementClient;
import com.slotcentral.jackpot.domain.Jackpot;
import com.slotcentral.jackpot.domain.JackpotContribution;
import com.slotcentral.jackpot.domain.JackpotWinHistory;
import com.slotcentral.jackpot.dto.*;
import com.slotcentral.jackpot.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class JackpotTransactionalOps {

    private static final Logger log = LoggerFactory.getLogger(JackpotTransactionalOps.class);

    private final JackpotRepository jackpotRepo;
    private final JackpotWinHistoryRepository winHistoryRepo;
    private final JackpotContributionRepository contributionRepo;
    private final FloorManagementClient floorClient;

    public JackpotTransactionalOps(JackpotRepository jackpotRepo,
                                   JackpotWinHistoryRepository winHistoryRepo,
                                   JackpotContributionRepository contributionRepo,
                                   FloorManagementClient floorClient) {
        this.jackpotRepo = jackpotRepo;
        this.winHistoryRepo = winHistoryRepo;
        this.contributionRepo = contributionRepo;
        this.floorClient = floorClient;
    }

    @Transactional
    public ContributeResponse executeContribute(Long jackpotId, ContributeRequest req) {
        Jackpot j = jackpotRepo.findById(jackpotId)
            .orElseThrow(() -> new EntityNotFoundException("Jackpot not found: " + jackpotId));
        BigDecimal contribution = j.getIncrementRate().multiply(req.betAmount());
        j.setCurrentAmount(j.getCurrentAmount().add(contribution));
        j.setLastIncrementedAt(Instant.now());
        Jackpot saved = jackpotRepo.save(j);

        if (req.spinId() != null) {
            JackpotContribution contrib = new JackpotContribution();
            contrib.setJackpotId(jackpotId);
            contrib.setSpinId(req.spinId());
            contrib.setBetAmount(req.betAmount());
            contrib.setContributionAmount(contribution);
            contrib.setEgmId(req.egmId());
            contributionRepo.save(contrib);
        }

        try {
            floorClient.broadcastRollup(jackpotId, saved.getCurrentAmount(), req.egmId());
        } catch (Exception ex) {
            log.warn("Floor rollup broadcast failed (non-fatal): {}", ex.getMessage());
        }

        return new ContributeResponse(JackpotResponse.from(saved), contribution, req.spinId(), false);
    }

    @Transactional
    public WinResponse executeWin(Long jackpotId, WinRequest req) {
        Jackpot j = jackpotRepo.findById(jackpotId)
            .orElseThrow(() -> new EntityNotFoundException("Jackpot not found: " + jackpotId));
        BigDecimal wonAmount = j.getCurrentAmount();

        j.setWonBy(req.uid());
        j.setWonAmount(wonAmount);
        j.setWonAt(Instant.now());
        j.setCurrentAmount(j.getBaseAmount());
        Jackpot saved = jackpotRepo.save(j);

        JackpotWinHistory history = new JackpotWinHistory();
        history.setJackpotId(jackpotId);
        history.setWonBy(req.uid());
        history.setWonAmount(wonAmount);
        history.setEgmId(req.egmId());
        history.setSpinId(req.spinId());
        JackpotWinHistory savedHistory = winHistoryRepo.save(history);

        try {
            floorClient.broadcastWin(jackpotId, wonAmount, req.egmId(), req.uid());
        } catch (Exception ex) {
            log.warn("Floor win broadcast failed (non-fatal): {}", ex.getMessage());
        }

        return new WinResponse(JackpotResponse.from(saved), WinHistoryResponse.from(savedHistory), false);
    }
}
