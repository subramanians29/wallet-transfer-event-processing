package com.wallet.transfer.repository;

import com.wallet.transfer.domain.TransferLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferLedgerRepository extends JpaRepository<TransferLedger, UUID> {

    boolean existsByTransferId(UUID transferId);

    Optional<TransferLedger> findByTransferId(UUID transferId);

}
