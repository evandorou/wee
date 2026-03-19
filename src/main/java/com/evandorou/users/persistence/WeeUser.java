package com.evandorou.users.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wee_user")
public class WeeUser {

    public static final BigDecimal DEFAULT_BALANCE_EUR = new BigDecimal("100.00");

    @Id
    private UUID id;

    @Column(name = "external_user_id", nullable = false, unique = true, length = 255)
    private String externalUserId;

    @Column(name = "balance_eur", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceEur;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WeeUser() {}

    public WeeUser(UUID id, String externalUserId, BigDecimal balanceEur, Instant createdAt) {
        this.id = id;
        this.externalUserId = externalUserId;
        this.balanceEur = balanceEur;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public BigDecimal getBalanceEur() {
        return balanceEur;
    }

    public void setBalanceEur(BigDecimal balanceEur) {
        this.balanceEur = balanceEur;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
