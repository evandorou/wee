package com.evandorou.bets.persistence;

import com.evandorou.users.persistence.WeeUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bet")
public class Bet {

    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private WeeUser user;

    @Column(name = "event_id", nullable = false, length = 512)
    private String eventId;

    @Column(name = "market_key", nullable = false, length = 128)
    private String marketKey;

    @Column(name = "outcome_id", nullable = false, length = 64)
    private String outcomeId;

    @Column(name = "stake_eur", nullable = false, precision = 19, scale = 2)
    private BigDecimal stakeEur;

    @Column(name = "odds", nullable = false)
    private int odds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BetStatus status;

    @Column(name = "payout_eur", precision = 19, scale = 2)
    private BigDecimal payoutEur;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected Bet() {}

    public Bet(
            UUID id,
            WeeUser user,
            String eventId,
            String marketKey,
            String outcomeId,
            BigDecimal stakeEur,
            int odds,
            BetStatus status,
            Instant placedAt
    ) {
        this.id = id;
        this.user = user;
        this.eventId = eventId;
        this.marketKey = marketKey;
        this.outcomeId = outcomeId;
        this.stakeEur = stakeEur;
        this.odds = odds;
        this.status = status;
        this.placedAt = placedAt;
    }

    public UUID getId() {
        return id;
    }

    public WeeUser getUser() {
        return user;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMarketKey() {
        return marketKey;
    }

    public String getOutcomeId() {
        return outcomeId;
    }

    public BigDecimal getStakeEur() {
        return stakeEur;
    }

    public int getOdds() {
        return odds;
    }

    public BetStatus getStatus() {
        return status;
    }

    public void setStatus(BetStatus status) {
        this.status = status;
    }

    public BigDecimal getPayoutEur() {
        return payoutEur;
    }

    public void setPayoutEur(BigDecimal payoutEur) {
        this.payoutEur = payoutEur;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }
}
