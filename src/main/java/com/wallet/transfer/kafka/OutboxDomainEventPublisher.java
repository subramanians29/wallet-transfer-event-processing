package com.wallet.transfer.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.domain.DomainEvent;
import com.wallet.transfer.domain.DomainEventPublisher;
import com.wallet.transfer.domain.OutboxEvent;
import com.wallet.transfer.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDomainEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        String payload = serialize(event);
        String eventType = event.getClass().getSimpleName();

        OutboxEvent outboxEvent = new OutboxEvent(
                event.aggregateType(),
                event.aggregateId(),
                eventType,
                payload
        );

        outboxEventRepository.save(outboxEvent);
        log.debug(
                "Domain Event written to outbox : type = {}, aggregateId = {}", eventType, event.aggregateId()
        );
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event : " + event.getClass().getSimpleName(), e);
        }
    }
}
