package com.mercia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.SettlementBatch;

public interface SettlementBatchRepository
        extends JpaRepository<SettlementBatch, Long> {
	
	List<SettlementBatch> findTop20ByOrderByStartedAtDesc();
}