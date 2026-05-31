package com.wallet.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Wallet(){}

    public Wallet(String ownerName, Money initialBalance) {
        this.ownerName = ownerName;
        this.balance = initialBalance.getAmount();
        this.currency = initialBalance.getCurrency().getCurrencyCode();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void debit(Money amount) {
        Money currentBalance = getMoney();
        Money newBalance = currentBalance.subtract(amount);

        this.balance = newBalance.getAmount();
        this.updatedAt = Instant.now();
    }

    public void credit(Money amount){
        Money currentBalance = getMoney();
        Money newBalance = currentBalance.add(amount);
        this.balance = newBalance.getAmount();
        this.updatedAt = Instant.now();
    }

    public boolean hasEnoughBalance(Money amount){
        return getMoney().isGreaterThanOrEqual(amount);
    }

    public Money getMoney(){
        return Money.of(balance, java.util.Currency.getInstance(currency));
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
