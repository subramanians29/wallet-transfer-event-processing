package com.wallet.transfer.domain;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent permits TransferCompletedEvent, TransferFailedEvent {

    UUID eventId();
    Instant occurredAt();
    String aggregateType();
    UUID aggregateId();
}
