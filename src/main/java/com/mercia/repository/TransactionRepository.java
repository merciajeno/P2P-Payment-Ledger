package com.mercia.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mercia.enums.TransactionStatus;
import com.mercia.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findByStatusAndSettlementDueAtLessThanEqual(TransactionStatus status, Instant time);

	Optional<Transaction> findByRequestId(String requestId);

	boolean existsBySenderWalletIdAndReceiverWalletIdAndCreatedAtAfter(Long senderWalletId, Long receiverWalletId,
			Instant createdAt);

	Page<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(Long senderWalletId,
			Long receiverWalletId, Pageable pageable);

	@Query("""
			SELECT t FROM Transaction t
			WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
			AND (:status IS NULL OR t.status = :status)
			AND (:from IS NULL OR t.createdAt >= :from)
			AND (:to IS NULL OR t.createdAt <= :to)
			ORDER BY t.createdAt DESC
			""")
	Page<Transaction> findByWalletFiltered(@Param("walletId") Long walletId, @Param("status") TransactionStatus status,
			@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

	// Count all transactions created today
	@Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
	long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

	// Count settled transactions today
	@Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.settledAt >= :start AND t.settledAt < :end")
	long countByStatusAndSettledAtBetween(@Param("status") TransactionStatus status, @Param("start") Instant start, @Param("end") Instant end);

	// Sum all amounts initiated today
	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
	BigDecimal sumAmountByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

	// Total pending amount across all users
	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'PENDING'")
	BigDecimal sumPendingAmount();

	// All transactions newest first — for admin monitoring
	Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

	// Pending transactions — for settlement monitor
	List<Transaction> findByStatus(TransactionStatus status);

}
