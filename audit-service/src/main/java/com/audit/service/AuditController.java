package com.audit.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final EventLogRepository eventLogRepository;

    public AuditController(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<EventLog>> getAllEvents(){
        return ResponseEntity.ok(eventLogRepository.findAllByOrderByRecordedAtDesc());
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<EventLog>> getByWallet(@PathVariable UUID walletId){
        return ResponseEntity.ok(
                eventLogRepository.findBySourceWalletIdOrTargetWalletIdOrderByRecordedAtDesc(walletId, walletId)
        );
    }

}
