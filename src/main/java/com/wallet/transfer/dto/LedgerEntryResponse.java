package com.wallet.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID transferId,
        UUID sourceWalletId,
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        String status,
        Instant transferredAt,
        Instant recordedAt
) {
}
