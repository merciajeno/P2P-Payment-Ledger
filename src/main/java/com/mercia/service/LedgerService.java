package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.enums.LedgerEntryReason;
import com.mercia.enums.LedgerEntryType;
import com.mercia.enums.LedgerHistoryType;
import com.mercia.enums.TransactionStatus;
import com.mercia.model.LedgerEntry;
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
    private static final ZoneId HISTORY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate EARLIEST_HISTORY_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate LATEST_HISTORY_DATE_EXCLUSIVE = LocalDate.of(10_000, 1, 1);

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

        return getWalletHistory(walletId, page, LedgerHistoryType.ALL, null, null, null);
    }

    @Transactional
    public Page<LedgerHistoryEntryResponse> getWalletHistory(
            Long walletId,
            int page,
            LedgerHistoryType type,
            TransactionStatus status,
            LocalDate from,
            LocalDate to) {

        Pageable pageable = PageRequest.of(page, 10);
        Instant fromInstant = (from == null ? EARLIEST_HISTORY_DATE : from)
                .atStartOfDay(HISTORY_ZONE).toInstant();
        Instant toInstant = (to == null ? LATEST_HISTORY_DATE_EXCLUSIVE : to.plusDays(1))
                .atStartOfDay(HISTORY_ZONE).toInstant();

        return ledgerRepo
            .findWalletHistoryFiltered(walletId, type.name(), status, fromInstant, toInstant, pageable)
            .map(entry -> {
                var transaction = entry.getTransaction();
                boolean isTransfer = transaction != null;
                boolean sent = isTransfer && transaction.getSenderWallet().getId().equals(walletId);
                return new LedgerHistoryEntryResponse(
                        entry.getId(),
                        entry.getEntryType(),
                        entry.getReason(),
                        entry.getAmount(),
                        entry.getCreatedAt(),
                        entry.getDescription(),
                        isTransfer ? sent ? "SENT" : "RECEIVED" : null,
                        isTransfer ? transaction.getStatus() : TransactionStatus.SUCCESS,
                        isTransfer ? transaction.getSettlementDueAt() : null,
                        isTransfer ? transaction.getRemark() : null);
            });
    }
}