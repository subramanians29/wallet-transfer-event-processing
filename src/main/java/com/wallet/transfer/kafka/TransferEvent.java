package com.wallet.transfer.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferEvent(
        UUID transferId,
        UUID sourceWalletId,
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        String status,
        Instant timestamp
) {
}
