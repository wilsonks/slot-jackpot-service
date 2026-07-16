package com.slotcentral.jackpot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "jackpot_win_history")
public class JackpotWinHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jackpotId;

    @Column(nullable = false, length = 100)
    private String wonBy;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wonAmount;

    @Column(length = 50)
    private String egmId;

    @Column(nullable = false, updatable = false)
    private Instant wonAt;

    @Column(length = 100, unique = true)
    private String spinId;

    @PrePersist
    void onCreate() {
        if (wonAt == null) wonAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJackpotId() { return jackpotId; }
    public void setJackpotId(Long jackpotId) { this.jackpotId = jackpotId; }
    public String getWonBy() { return wonBy; }
    public void setWonBy(String wonBy) { this.wonBy = wonBy; }
    public BigDecimal getWonAmount() { return wonAmount; }
    public void setWonAmount(BigDecimal wonAmount) { this.wonAmount = wonAmount; }
    public String getEgmId() { return egmId; }
    public void setEgmId(String egmId) { this.egmId = egmId; }
    public Instant getWonAt() { return wonAt; }
    public void setWonAt(Instant wonAt) { this.wonAt = wonAt; }
    public String getSpinId() { return spinId; }
    public void setSpinId(String spinId) { this.spinId = spinId; }
}
