package com.wallet.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_ledger")
public class TransferLedger {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID transferId;

    @Column(nullable = false)
    private UUID sourceWalletId;

    @Column(nullable = false)
    private UUID targetWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant transferredAt;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    public TransferLedger() {
    }

    public TransferLedger(UUID transferId, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String currency, String status, Instant transferredAt) {
        this.transferId = transferId;
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.transferredAt = transferredAt;
        this.recordedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
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

    public String getStatus() {
        return status;
    }

    public Instant getTransferredAt() {
        return transferredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
