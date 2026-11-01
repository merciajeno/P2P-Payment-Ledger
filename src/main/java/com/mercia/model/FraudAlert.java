package com.mercia.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.FraudAlertType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "fraud_alerts",
    indexes = {
        @Index(name = "idx_fraud_alert_status", columnList = "status"),
        @Index(name = "idx_fraud_alert_wallet", columnList = "wallet_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The wallet that triggered the alert (always the sender)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // The transaction that triggered the alert — null for DAILY_LIMIT_EXCEEDED
    // (since the transaction is blocked before it's created)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudAlertStatus status = FraudAlertStatus.PENDING;

    // Amount involved — useful for display without joining transaction
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 250)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Filled in when admin acts on it
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
