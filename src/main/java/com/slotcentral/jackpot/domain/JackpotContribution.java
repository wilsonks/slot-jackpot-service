package com.slotcentral.jackpot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "jackpot_contributions")
public class JackpotContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jackpotId;

    @Column(nullable = false, length = 100, unique = true)
    private String spinId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal betAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal contributionAmount;

    @Column(length = 50)
    private String egmId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJackpotId() { return jackpotId; }
    public void setJackpotId(Long jackpotId) { this.jackpotId = jackpotId; }
    public String getSpinId() { return spinId; }
    public void setSpinId(String spinId) { this.spinId = spinId; }
    public BigDecimal getBetAmount() { return betAmount; }
    public void setBetAmount(BigDecimal betAmount) { this.betAmount = betAmount; }
    public BigDecimal getContributionAmount() { return contributionAmount; }
    public void setContributionAmount(BigDecimal contributionAmount) { this.contributionAmount = contributionAmount; }
    public String getEgmId() { return egmId; }
    public void setEgmId(String egmId) { this.egmId = egmId; }
    public Instant getCreatedAt() { return createdAt; }
}
