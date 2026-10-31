package com.mercia.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "daily_summary",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_daily_summary_date",
            columnNames = "summary_date"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    // Transfers
    @Column(nullable = false)
    private Integer transfersInitiated = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountInitiated = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer transfersSettled = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountSettled = BigDecimal.ZERO;

    // Fraud
    @Column(nullable = false)
    private Integer fraudAlertsRaised = 0;

    // Users
    @Column(nullable = false)
    private Integer newUserRegistrations = 0;

    // Pending across all users at time of run
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPendingSettlementAmount = BigDecimal.ZERO;

    // Reconciliation
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean reconciliationPassed = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
