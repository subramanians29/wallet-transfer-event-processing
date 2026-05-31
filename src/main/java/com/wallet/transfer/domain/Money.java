package com.wallet.transfer.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency){

        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");

        if(amount.signum() < 0){
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }

        return new Money(amount.stripTrailingZeros(), currency);
    }

    public static Money of(BigDecimal amount){
        return of(amount, Currency.getInstance("EUR"));
    }

    private void assertSameCurrency(Money other){
        if(!this.currency.equals(other.currency)){
            throw new IllegalArgumentException(
                    "Currency mismatch : " + this.currency + " vs " + other.currency
            );
        }
    }

    public Money add(Money other){
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other){
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);

        if(result.signum() < 0){
            throw new IllegalArgumentException(
                    "Insufficient balance : cannot subtract " + other.amount + " from " + this.amount
            );
        }
        return new Money(result, this.currency);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public BigDecimal getAmount(){
        return amount;
    }

    public Currency getCurrency(){
        return currency;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
