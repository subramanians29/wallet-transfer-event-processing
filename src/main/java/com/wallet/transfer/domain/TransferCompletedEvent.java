package com.wallet.transfer.domain;

import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID sourceWalletId,
        UUID targetWalletId,
        Money amount,
        Instant occurredAt
) implements DomainEvent {

    public TransferCompletedEvent(UUID transferId, UUID sourceWalletId, UUID targetWalletId, Money amount) {
        this(UUID.randomUUID(), transferId, sourceWalletId, targetWalletId, amount, Instant.now());
    }

    @Override
    public String aggregateType(){
        return "Transfer";
    }
}
