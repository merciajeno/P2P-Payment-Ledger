package com.mercia.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.mercia.model.*;
import com.mercia.repository.LedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerRepo;

    @Transactional
    public void recordEntry(
            Wallet wallet,
            Transaction transaction,
            LedgerEntryType entryType,
            LedgerEntryReason reason,
            BigDecimal amount,
            String description) {

        LedgerEntry entry = new LedgerEntry();

        entry.setWallet(wallet);
        entry.setTransaction(transaction);
        entry.setEntryType(entryType);
        entry.setReason(reason);
        entry.setAmount(amount);
        entry.setDescription(description);
       log.info("saved into ledger");
       ledgerRepo.save(entry);
    }
}