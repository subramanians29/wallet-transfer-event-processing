package com.wallet.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.domain.OutboxEvent;
import com.wallet.transfer.kafka.TransferEvent;
import com.wallet.transfer.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper, @Value("${app.kafka.topic.transfer-events}") String topic) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents(){
        log.info("Inside Publish Pending events.........>>>>>>");
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

        for(OutboxEvent event : pending){
            try{
                TransferEvent transferEvent = toKafkaEvent(event);
                kafkaTemplate.send(topic, event.getAggregateId().toString(), transferEvent).get();
                event.markProcessed();
                log.debug("Published outbox event : {}", event.getId());
            } catch(JsonProcessingException ex){
                log.error("Failed to deserialize outbox event payload : {}", event.getId(), ex);
                event.markFailed();
            } catch(Exception e) {
                log.error("Failed to publish outbox event : {}", event.getId(), e);
                break;
            }

        }

    }

    private TransferEvent toKafkaEvent(OutboxEvent outboxEvent) throws JsonProcessingException {

        JsonNode node = objectMapper.readTree(outboxEvent.getPayload());

        return new TransferEvent(
                UUID.fromString(node.get("aggregateId").asText()),
                UUID.fromString(node.get("sourceWalletId").asText()),
                UUID.fromString(node.get("targetWalletId").asText()),
                new BigDecimal(node.get("amount").get("amount").asText()),
                node.get("amount").get("currency").asText(),
                outboxEvent.getEventType().equals("TransferCompletedEvent") ? "COMPLETED" : "FAILED",
                Instant.parse(node.get("occurredAt").asText())
        );

    }
}
