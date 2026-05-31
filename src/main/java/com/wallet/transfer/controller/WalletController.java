package com.wallet.transfer.controller;

import com.wallet.transfer.dto.CreateWalletRequest;
import com.wallet.transfer.dto.TransferRequest;
import com.wallet.transfer.dto.TransferResponse;
import com.wallet.transfer.dto.WalletResponse;
import com.wallet.transfer.repository.WalletRepository;
import com.wallet.transfer.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet", description = "Wallet management and transfer operations")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request
    ) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get Wallet details and balance")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID walletId){
        return ResponseEntity.ok(walletService.getWallet(walletId));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer money between wallets")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-key") String idempotencyKey
    ) {
        TransferResponse response = walletService.transfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
