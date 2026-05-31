package com.wallet.transfer.kafka;

import com.wallet.transfer.domain.TransferLedger;
import com.wallet.transfer.repository.TransferLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TranserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TranserEventConsumer.class);

    private final TransferLedgerRepository ledgerRepository;

    public TranserEventConsumer(TransferLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
            topics = "${app.kafka.topic.transfer-events}", groupId = "wallet-transfer-group"
    )
    @Transactional
    public void handleTransferEvent(
            TransferEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        if(ledgerRepository.existsByTransferId(event.transferId())){
            log.info("Duplicate event skipped: transferId = {}", event.transferId());
            return;
        }

        TransferLedger entry = new TransferLedger(
                event.transferId(),
                event.sourceWalletId(),
                event.targetWalletId(),
                event.amount(),
                event.currency(),
                event.status(),
                event.timestamp()
                );

        ledgerRepository.save(entry);

        log.info("Ledger entry recorded: transferId = {}, source = {}, target = {}, amount = {} {}",
                event.transferId(), event.sourceWalletId(), event.targetWalletId(), event.amount(), event.currency());
    }

    @DltHandler
    public void handleDlt(
            TransferEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ){
        log.error(
                "Transfer event moved to DLT: transferId = {}, source = {}, target = {}, amount = {}",
                event.transferId(), event.sourceWalletId(), event.targetWalletId(), event.amount()
        );
    }

}
