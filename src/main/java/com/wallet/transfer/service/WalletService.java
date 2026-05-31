package com.wallet.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.domain.*;
import com.wallet.transfer.dto.CreateWalletRequest;
import com.wallet.transfer.dto.TransferRequest;
import com.wallet.transfer.dto.TransferResponse;
import com.wallet.transfer.dto.WalletResponse;
import com.wallet.transfer.exception.DuplicateRequestException;
import com.wallet.transfer.exception.InsufficientBalanceException;
import com.wallet.transfer.exception.WalletNotFoundException;
import com.wallet.transfer.kafka.TransferEvent;
import com.wallet.transfer.repository.OutboxEventRepository;
import com.wallet.transfer.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final IdempotencyService idempotencyService;

    public WalletService(WalletRepository walletRepository, DomainEventPublisher domainEventPublisher, IdempotencyService idempotencyService) {
        this.walletRepository = walletRepository;
        this.idempotencyService = idempotencyService;
        this.domainEventPublisher = domainEventPublisher;
    }

    public WalletResponse toResponse(Wallet wallet){
        return new WalletResponse(
                wallet.getId(),
                wallet.getOwnerName(),
                wallet.getBalance(),
                wallet.getCreatedAt()
        );
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request){
        Money initialBalance = Money.of(request.initialBalance());
        Wallet wallet = new Wallet(request.ownerName(), initialBalance);
        wallet = walletRepository.save(wallet);
        log.info("Created Wallet : {} for owner : {}", wallet.getId(), wallet.getOwnerName());
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID walletId){
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
        return toResponse(wallet);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String idempotencyKey){

        if(request.sourceWalletId().equals(request.targetWalletId())){
            throw new IllegalArgumentException("Source and target wallet must be different");
        }

        if(idempotencyService.isDuplicate(idempotencyKey)){
            throw new DuplicateRequestException(idempotencyKey);
        }

        try {
            UUID transferId = UUID.randomUUID();
            Money transferAmount = Money.of(request.amount());

            Wallet source = walletRepository.findByIdWithLock(request.sourceWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(request.sourceWalletId()));

            Wallet target = walletRepository.findByIdWithLock(request.targetWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(request.targetWalletId()));

            if(!source.hasEnoughBalance(transferAmount)){
                throw new InsufficientBalanceException(
                        "Wallet " + source.getId() + "has insufficient balance for transfer of " + request.amount()
                );
            }

            source.debit(transferAmount);
            target.credit(transferAmount);

            walletRepository.save(source);
            walletRepository.save(target);

            TransferCompletedEvent domainEvent = new TransferCompletedEvent(
                    transferId, request.sourceWalletId(), request.targetWalletId(), transferAmount
            );
            domainEventPublisher.publish(domainEvent);

            idempotencyService.markCompleted(idempotencyKey, transferId.toString());

            log.info(
                    "Transfer completed : {} from {} to {} amount {}",
                    transferId, request.sourceWalletId(), request.targetWalletId(), request.amount()
            );

            return new TransferResponse(
                    transferId,
                    request.sourceWalletId(),
                    request.targetWalletId(),
                    request.amount(),
                    "COMPLETED",
                    domainEvent.occurredAt()
            );
        } catch (WalletNotFoundException | InsufficientBalanceException | IllegalArgumentException e){
            idempotencyService.remove(idempotencyKey);
            throw e;
        }

    }

}
