package com.audit.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class WalletServiceClient {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceClient.class);

    private final RestClient restClient;

    public WalletServiceClient(@Value("${app.wallet-service.base-url}") String baseUrl){
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "wallet-service", fallbackMethod = "fallbackGetOwnerName")
    @Retry(name = "wallet-service")
    public String getOwnerName(UUID walletId){
        log.info("Calling wallet-service for wallet : {}", walletId);
        WalletResponse response = restClient.get()
                .uri("/api/v1/wallets/{id}", walletId)
                .retrieve()
                .body(WalletResponse.class);

        log.info("Got owner : {}", response != null ? response.ownerName() : "unknown");

        return response != null ? response.ownerName() : "unknown";
    }

    private String fallbackGetOwnerName(UUID walletId, Throwable t){
        log.warn("Circuit breaker gallback for wallet {}: {}", walletId, t.getMessage());
        return "unavailable";
    }

    record WalletResponse(UUID id, String ownerName, BigDecimal balance, Instant createdAt) {}

}
