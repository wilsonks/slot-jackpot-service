package com.slotcentral.jackpot.repository;

import com.slotcentral.jackpot.domain.Jackpot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JackpotRepository extends JpaRepository<Jackpot, Long> {
    Page<Jackpot> findByIsActive(boolean isActive, Pageable pageable);
}
