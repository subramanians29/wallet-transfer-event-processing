package com.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final EventLogRepository eventLogRepository;
    private final WalletServiceClient walletServiceClient;

    public TransferEventListener(EventLogRepository eventLogRepository, WalletServiceClient walletServiceClient) {
        this.eventLogRepository = eventLogRepository;
        this.walletServiceClient = walletServiceClient;
    }

    @KafkaListener(topics = "${app.kafka.topic.transfer-events}", groupId = "audit-service-group")
    @Transactional
    public void onTransferEvent(TransferEvent event){

        if(eventLogRepository.existsByTransferId(event.transferId())){
            log.info("Duplicate event skipped : transferId = {}", event.transferId());
            return;
        }

        EventLog entry = new EventLog(
                event.transferId(),
                "TRANSFER_" + event.status(),
                event.sourceWalletId(),
                event.targetWalletId(),
                event.amount(),
                event.currency() != null ? event.currency() : "EUR",
                event.timestamp()
        );

        //Enrich with owner names via circuit breaker protected call
        String sourceOwner = walletServiceClient.getOwnerName(event.sourceWalletId());
        String targetOwner = walletServiceClient.getOwnerName(event.targetWalletId());

        eventLogRepository.save(entry);
        log.info(
                "Audit log recorded : transferId = {}, {} -> {}, amount = {}, {}",
                event.transferId(), sourceOwner, targetOwner, event.amount(), event.currency()
        );

    }
}
