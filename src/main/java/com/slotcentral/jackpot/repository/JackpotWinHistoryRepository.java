package com.slotcentral.jackpot.repository;

import com.slotcentral.jackpot.domain.JackpotWinHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotWinHistoryRepository extends JpaRepository<JackpotWinHistory, Long> {
    Page<JackpotWinHistory> findByJackpotId(Long jackpotId, Pageable pageable);
    Optional<JackpotWinHistory> findBySpinId(String spinId);
}
