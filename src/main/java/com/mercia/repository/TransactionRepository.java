package com.mercia.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	 List<Transaction> findByStatusAndSettlementDueAtLessThanEqual(
	            TransactionStatus status,
	            Instant time);
	 
	 Optional<Transaction> findByRequestId(String requestId);
	 
	 boolean existsBySenderWalletIdAndReceiverWalletIdAndAmountAndCreatedAtAfter(
		        Long senderWalletId,
		        Long receiverWalletId,
		        BigDecimal amount,
		        Instant createdAt
		);
}
