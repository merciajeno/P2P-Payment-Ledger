package com.mercia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;

@Repository
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

}
