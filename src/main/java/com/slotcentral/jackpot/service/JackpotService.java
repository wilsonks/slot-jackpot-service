package com.slotcentral.jackpot.service;

import com.slotcentral.jackpot.client.FloorManagementClient;
import com.slotcentral.jackpot.domain.Jackpot;
import com.slotcentral.jackpot.dto.*;
import com.slotcentral.jackpot.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JackpotService {

    private static final Logger log = LoggerFactory.getLogger(JackpotService.class);
    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 5;

    private final JackpotRepository jackpotRepo;
    private final JackpotWinHistoryRepository winHistoryRepo;
    private final JackpotContributionRepository contributionRepo;
    private final JackpotTransactionalOps transactionalOps;
    private final FloorManagementClient floorClient;

    public JackpotService(JackpotRepository jackpotRepo,
                          JackpotWinHistoryRepository winHistoryRepo,
                          JackpotContributionRepository contributionRepo,
                          JackpotTransactionalOps transactionalOps,
                          FloorManagementClient floorClient) {
        this.jackpotRepo = jackpotRepo;
        this.winHistoryRepo = winHistoryRepo;
        this.contributionRepo = contributionRepo;
        this.transactionalOps = transactionalOps;
        this.floorClient = floorClient;
    }

    @Transactional
    public JackpotResponse createJackpot(CreateJackpotRequest req) {
        Jackpot j = new Jackpot();
        j.setName(req.name());
        j.setBaseAmount(req.baseAmount());
        j.setIncrementRate(req.incrementRate());
        j.setCurrentAmount(req.baseAmount());
        j.setActive(true);
        return JackpotResponse.from(jackpotRepo.save(j));
    }

    @Transactional(readOnly = true)
    public Page<JackpotResponse> getAllJackpots(Boolean isActive, Pageable pageable) {
        if (isActive != null) {
            return jackpotRepo.findByIsActive(isActive, pageable).map(JackpotResponse::from);
        }
        return jackpotRepo.findAll(pageable).map(JackpotResponse::from);
    }

    @Transactional(readOnly = true)
    public JackpotResponse getJackpotById(Long id) {
        return JackpotResponse.from(findJackpotOrThrow(id));
    }

    @Transactional
    public JackpotResponse updateJackpot(Long id, UpdateJackpotRequest req) {
        Jackpot j = findJackpotOrThrow(id);
        if (req.name() != null) j.setName(req.name());
        if (req.baseAmount() != null) j.setBaseAmount(req.baseAmount());
        if (req.incrementRate() != null) j.setIncrementRate(req.incrementRate());
        if (req.isActive() != null) j.setActive(req.isActive());
        return JackpotResponse.from(jackpotRepo.save(j));
    }

    @Transactional
    public void deleteJackpot(Long id) {
        if (!jackpotRepo.existsById(id)) {
            throw new EntityNotFoundException("Jackpot not found: " + id);
        }
        jackpotRepo.deleteById(id);
    }

    public ContributeResponse contribute(Long jackpotId, ContributeRequest req) {
        if (req.spinId() != null) {
            var existing = contributionRepo.findBySpinId(req.spinId());
            if (existing.isPresent()) {
                log.info("Idempotent replay for contribute spinId={}", req.spinId());
                return new ContributeResponse(
                    JackpotResponse.from(findJackpotOrThrow(jackpotId)),
                    existing.get().getContributionAmount(),
                    req.spinId(),
                    true
                );
            }
        }

        int retryCount = 0;
        while (true) {
            try {
                return transactionalOps.executeContribute(jackpotId, req);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                if (retryCount >= MAX_OPTIMISTIC_LOCK_RETRIES) {
                    log.error("Optimistic lock exhausted for jackpot {} after {} retries", jackpotId, MAX_OPTIMISTIC_LOCK_RETRIES);
                    throw new OptimisticLockRetryExhaustedException("Concurrent update conflict on jackpot " + jackpotId);
                }
                retryCount++;
                log.warn("Optimistic lock conflict on jackpot {}, retry {}/{}", jackpotId, retryCount, MAX_OPTIMISTIC_LOCK_RETRIES);
            }
        }
    }

    public WinResponse recordWin(Long jackpotId, WinRequest req) {
        if (req.spinId() != null) {
            var existing = winHistoryRepo.findBySpinId(req.spinId());
            if (existing.isPresent()) {
                log.info("Idempotent replay for win spinId={}", req.spinId());
                return new WinResponse(
                    JackpotResponse.from(findJackpotOrThrow(jackpotId)),
                    WinHistoryResponse.from(existing.get()),
                    true
                );
            }
        }
        return transactionalOps.executeWin(jackpotId, req);
    }

    @Transactional(readOnly = true)
    public Page<WinHistoryResponse> getWinHistory(Long jackpotId, Pageable pageable) {
        findJackpotOrThrow(jackpotId);
        return winHistoryRepo.findByJackpotId(jackpotId, pageable).map(WinHistoryResponse::from);
    }

    @Transactional
    public JackpotResponse adminReset(Long jackpotId) {
        Jackpot j = findJackpotOrThrow(jackpotId);
        j.setCurrentAmount(j.getBaseAmount());
        Jackpot saved = jackpotRepo.save(j);
        try {
            floorClient.broadcastReset(jackpotId, j.getBaseAmount());
        } catch (Exception ex) {
            log.warn("Floor reset broadcast failed (non-fatal): {}", ex.getMessage());
        }
        return JackpotResponse.from(saved);
    }

    private Jackpot findJackpotOrThrow(Long id) {
        return jackpotRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Jackpot not found: " + id));
    }

    public static class OptimisticLockRetryExhaustedException extends RuntimeException {
        public OptimisticLockRetryExhaustedException(String message) {
            super(message);
        }
    }
}
