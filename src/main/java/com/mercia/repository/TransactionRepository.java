package com.mercia.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	 List<Transaction> findByStatusAndSettlementDueAtLessThanEqual(
	            TransactionStatus status,
	            Instant time);
}
