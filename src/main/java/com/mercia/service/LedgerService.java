package com.mercia.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.model.LedgerEntry;
import com.mercia.model.LedgerEntryReason;
import com.mercia.model.LedgerEntryType;
import com.mercia.model.Transaction;
import com.mercia.model.Wallet;
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

    @Transactional
    public Page<LedgerHistoryEntryResponse> getWalletHistory(
            Long walletId, int page) {

        Pageable pageable = PageRequest.of(page, 10);

        return ledgerRepo
            .findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
            .map(entry -> new LedgerHistoryEntryResponse(
                    entry.getId(),
                    entry.getEntryType(),
                    entry.getReason(),
                    entry.getAmount(),
                    entry.getCreatedAt(),
                    entry.getDescription()));
    }
}