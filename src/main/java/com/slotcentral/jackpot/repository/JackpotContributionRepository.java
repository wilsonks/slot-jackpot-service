package com.slotcentral.jackpot.repository;

import com.slotcentral.jackpot.domain.JackpotContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, Long> {
    Optional<JackpotContribution> findBySpinId(String spinId);
}
