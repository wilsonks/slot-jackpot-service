package com.slotcentral.jackpot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "jackpots")
public class Jackpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal incrementRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(length = 100)
    private String wonBy;

    @Column(precision = 19, scale = 4)
    private BigDecimal wonAmount;

    @Column
    private Instant wonAt;

    @Column
    private Instant lastIncrementedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getIncrementRate() { return incrementRate; }
    public void setIncrementRate(BigDecimal incrementRate) { this.incrementRate = incrementRate; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getWonBy() { return wonBy; }
    public void setWonBy(String wonBy) { this.wonBy = wonBy; }
    public BigDecimal getWonAmount() { return wonAmount; }
    public void setWonAmount(BigDecimal wonAmount) { this.wonAmount = wonAmount; }
    public Instant getWonAt() { return wonAt; }
    public void setWonAt(Instant wonAt) { this.wonAt = wonAt; }
    public Instant getLastIncrementedAt() { return lastIncrementedAt; }
    public void setLastIncrementedAt(Instant lastIncrementedAt) { this.lastIncrementedAt = lastIncrementedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
