package com.mercia.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settlement_batches")
@Getter
@Setter
@NoArgsConstructor
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private Integer totalProcessed = 0;

    @Column(nullable = false)
    private Integer settledCount = 0;

    @Column(nullable = false)
    private Integer failedCount = 0;

    @Column(nullable = false)
    private Integer declinedCount = 0;

    @Column(
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal totalAmountSettled = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }
}