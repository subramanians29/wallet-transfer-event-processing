package com.wallet.transfer.domain;

import java.time.Instant;
import java.util.UUID;

public record TransferFailedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID sourceWalletId,
        UUID targetWalletId,
        Money amount,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public TransferFailedEvent(UUID eventId, UUID aggregateId, UUID sourceWalletId, UUID targetWalletId, Money amount, String reason) {
        this(eventId, aggregateId, sourceWalletId, targetWalletId, amount, reason, Instant.now());
    }

    @Override
    public String aggregateType(){
        return "Transfer";
    }
}
