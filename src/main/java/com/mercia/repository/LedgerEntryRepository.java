package com.mercia.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mercia.enums.TransactionStatus;
import com.mercia.model.LedgerEntry;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId,Pageable pageable);

    @Query(value = """
            SELECT e FROM LedgerEntry e
            LEFT JOIN e.transaction t
            WHERE e.wallet.id = :walletId
            AND (:from IS NULL OR COALESCE(t.createdAt, e.createdAt) >= :from)
            AND (:to IS NULL OR COALESCE(t.createdAt, e.createdAt) < :to)
            AND (:type = 'ALL'
                OR (:type = 'SENT' AND t.senderWallet.id = :walletId)
                OR (:type = 'RECEIVED' AND t.receiverWallet.id = :walletId))
            AND (:status IS NULL
                OR (:status = com.mercia.enums.TransactionStatus.SUCCESS
                    AND (t IS NULL OR t.status = :status))
                OR t.status = :status
                OR (:status = com.mercia.enums.TransactionStatus.FAILED
                    AND t.status = com.mercia.enums.TransactionStatus.DECLINED))
            ORDER BY COALESCE(t.createdAt, e.createdAt) DESC, e.id DESC
            """,
            countQuery = """
            SELECT COUNT(e) FROM LedgerEntry e
            LEFT JOIN e.transaction t
            WHERE e.wallet.id = :walletId
            AND (:from IS NULL OR COALESCE(t.createdAt, e.createdAt) >= :from)
            AND (:to IS NULL OR COALESCE(t.createdAt, e.createdAt) < :to)
            AND (:type = 'ALL'
                OR (:type = 'SENT' AND t.senderWallet.id = :walletId)
                OR (:type = 'RECEIVED' AND t.receiverWallet.id = :walletId))
            AND (:status IS NULL
                OR (:status = com.mercia.enums.TransactionStatus.SUCCESS
                    AND (t IS NULL OR t.status = :status))
                OR t.status = :status
                OR (:status = com.mercia.enums.TransactionStatus.FAILED
                    AND t.status = com.mercia.enums.TransactionStatus.DECLINED))
            """)
    Page<LedgerEntry> findWalletHistoryFiltered(
            @Param("walletId") Long walletId,
            @Param("type") String type,
            @Param("status") TransactionStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}