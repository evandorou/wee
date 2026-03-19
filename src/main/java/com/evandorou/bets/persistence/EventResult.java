package com.evandorou.bets.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_result")
public class EventResult {

    @Id
    @Column(name = "event_id", nullable = false, length = 512)
    private String eventId;

    @Column(name = "winning_driver_number", nullable = false)
    private int winningDriverNumber;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected EventResult() {}

    public EventResult(String eventId, int winningDriverNumber, Instant recordedAt) {
        this.eventId = eventId;
        this.winningDriverNumber = winningDriverNumber;
        this.recordedAt = recordedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public int getWinningDriverNumber() {
        return winningDriverNumber;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
