package com.wallet.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String ownerName,
        BigDecimal balance,
        Instant createdAt
) {
}
