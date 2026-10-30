package com.mercia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.LedgerEntry;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}