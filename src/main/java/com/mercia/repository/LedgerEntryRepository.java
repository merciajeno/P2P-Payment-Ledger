package com.mercia.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.LedgerEntry;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId,Pageable pageable);

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}