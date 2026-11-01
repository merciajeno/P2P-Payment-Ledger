package com.mercia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.FraudAlertType;
import com.mercia.model.FraudAlert;

public interface FraudAlertRepository
        extends JpaRepository<FraudAlert, Long> {

    // Admin fraud review queue — all unresolved alerts
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlertStatus status);

    // Check if a specific transaction already has a high-value alert
    // (prevents duplicates if settlement job re-runs)
    boolean existsByTransactionIdAndAlertType(
        Long transactionId,
        FraudAlertType alertType
    );
    

    // For daily summary count
    long countByStatusAndCreatedAtBetween(
        FraudAlertStatus status,
        java.time.Instant start,
        java.time.Instant end
    );
    
 // Count all alerts raised in a time window regardless of status
    @Query("""
        SELECT COUNT(fa) FROM FraudAlert fa
        WHERE fa.createdAt >= :start
        AND fa.createdAt < :end
        """)
    long countByCreatedAtBetween(
        @Param("start") java.time.Instant start,
        @Param("end") java.time.Instant end
    );
    
    long countByStatus(FraudAlertStatus status);


}
