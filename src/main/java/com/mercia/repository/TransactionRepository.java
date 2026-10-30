package com.mercia.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	 List<Transaction> findByStatusAndSettlementDueAtLessThanEqual(
	            TransactionStatus status,
	            Instant time);
	 
	 Optional<Transaction> findByRequestId(String requestId);

    @Query("""
            select t from Transaction t
            where (t.senderWallet.id = :walletId or t.receiverWallet.id = :walletId)
              and (:fromDate is null or t.createdAt >= :fromDate)
              and (:toExclusive is null or t.createdAt < :toExclusive)
              and (:direction = 'ALL'
                   or (:direction = 'SENT' and t.senderWallet.id = :walletId)
                   or (:direction = 'RECEIVED' and t.receiverWallet.id = :walletId))
              and (:status is null
                   or t.status = :status
                   or (:includeDeclined = true and t.status = com.mercia.model.TransactionStatus.DECLINED))
            """)
    Page<Transaction> findHistory(
            @Param("walletId") Long walletId,
            @Param("fromDate") Instant fromDate,
            @Param("toExclusive") Instant toExclusive,
            @Param("direction") String direction,
            @Param("status") TransactionStatus status,
            @Param("includeDeclined") boolean includeDeclined,
            Pageable pageable);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.senderWallet.id = :walletId and t.status = :status
            """)
    BigDecimal sumSentByWalletAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.receiverWallet.id = :walletId and t.status = :status
            """)
    BigDecimal sumReceivedByWalletAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status);

    @Query("""
            select count(t) from Transaction t
            where (t.senderWallet.id = :walletId or t.receiverWallet.id = :walletId)
              and t.createdAt >= :fromDate and t.createdAt < :toExclusive
            """)
    long countForWalletInPeriod(
            @Param("walletId") Long walletId,
            @Param("fromDate") Instant fromDate,
            @Param("toExclusive") Instant toExclusive);
	 
	 boolean existsBySenderWalletIdAndReceiverWalletIdAndAmountAndCreatedAtAfter(
		        Long senderWalletId,
		        Long receiverWalletId,
		        BigDecimal amount,
		        Instant createdAt
		);
}
