package com.audit.service;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_log")
public class EventLog {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID transferId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID sourceWalletId;

    @Column(nullable = false)
    private UUID targetWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String sourceOwner;

    private String targetOwner;

    @Column(nullable = false, updatable = false)
    private Instant eventTimestamp;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    protected EventLog(){}

    public EventLog(UUID transferId, String eventType, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String currency, Instant eventTimestamp) {

        this.transferId = transferId;
        this.eventType = eventType;
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.recordedAt = Instant.now();
    }

    public void enrichOwners(String sourceOwner, String targetOwner){
        this.sourceOwner = sourceOwner;
        this.targetOwner = targetOwner;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getTargetWalletId() {
        return targetWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public String getTargetOwner() {
        return targetOwner;
    }

    public Instant getEventTimeStamp() {
        return eventTimestamp;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
