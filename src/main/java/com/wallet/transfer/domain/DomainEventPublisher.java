package com.wallet.transfer.domain;

public interface DomainEventPublisher {

    void publish(DomainEvent event);

}
