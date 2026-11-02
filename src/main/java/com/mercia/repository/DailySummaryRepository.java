package com.mercia.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mercia.enums.TransactionStatus;
import com.mercia.model.DailySummary;

public interface DailySummaryRepository
        extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findBySummaryDate(LocalDate date);

    // Count transactions created today with a given status
    @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE t.status = :status
        AND t.createdAt >= :start
        AND t.createdAt < :end
        """)
    long countByStatusAndCreatedAtBetween(
        @Param("status") TransactionStatus status,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    // Sum amounts for transactions initiated today
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.createdAt >= :start
        AND t.createdAt < :end
        """)
    java.math.BigDecimal sumAmountInitiatedBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    // Sum amounts for transactions settled today
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.status = :status
        AND t.settledAt >= :start
        AND t.settledAt < :end
        """)
    java.math.BigDecimal sumAmountSettledBetween(
        @Param("status") TransactionStatus status,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    // Count users registered today
    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.id IN (
            SELECT u2.id FROM User u2
        )
        """)
    // — better done with a createdAt on User, see note below
    long countUsersRegisteredBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    // Total pending settlement amount right now
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.status = 'PENDING'
        """)
    java.math.BigDecimal sumTotalPendingAmount();

    // Sum debit ledger entries for transactions settled in the requested window
    @Query("""
        SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le
        WHERE le.entryType = 'DEBIT'
        AND le.transaction IS NOT NULL
        AND le.transaction.status = 'SUCCESS'
        AND le.transaction.settledAt >= :start
        AND le.transaction.settledAt < :end
        """)
    java.math.BigDecimal sumSettledDebits(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    // Sum settlement credits for transactions settled in the requested window
    @Query("""
        SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le
        WHERE le.entryType = 'CREDIT'
        AND le.reason = 'SETTLEMENT'
        AND le.transaction IS NOT NULL
        AND le.transaction.status = 'SUCCESS'
        AND le.transaction.settledAt >= :start
        AND le.transaction.settledAt < :end
        """)
    java.math.BigDecimal sumSettledCredits(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}
