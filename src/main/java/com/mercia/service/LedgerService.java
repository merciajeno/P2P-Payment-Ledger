package com.mercia.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.mercia.model.*;
import com.mercia.repository.LedgerEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerRepo;

    public LedgerEntry recordEntry(
            Wallet wallet,
            Transaction transaction,
            LedgerEntryType entryType,
            LedgerEntryReason reason,
            BigDecimal amount,
            String description) {

        if (wallet == null) {
            throw new IllegalArgumentException("Wallet cannot be null");
        }

        if (entryType == null || reason == null) {
            throw new IllegalArgumentException("Entry type and reason are required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger amount must be positive");
        }

        LedgerEntry entry = new LedgerEntry();

        entry.setWallet(wallet);
        entry.setTransaction(transaction);
        entry.setEntryType(entryType);
        entry.setReason(reason);
        entry.setAmount(amount);
        entry.setDescription(description);

        return ledgerRepo.save(entry);
    }
}