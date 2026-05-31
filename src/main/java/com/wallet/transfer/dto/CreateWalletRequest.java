package com.wallet.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateWalletRequest(
        @NotBlank(message = "Owner name is required") String ownerName,

        @DecimalMin(value = "0.0", message = "Initial balance must be non negative") BigDecimal initialBalance
        ) {

    public CreateWalletRequest{
        if(initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }
    }
}
