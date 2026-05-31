package com.audit.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, UUID> {

    boolean existsByTransferId(UUID transferId);

    List<EventLog> findAllByOrderByRecordedAtDesc();

    List<EventLog> findBySourceWalletIdOrTargetWalletIdOrderByRecordedAtDesc(UUID sourceId, UUID targetId);

}
