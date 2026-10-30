package com.mercia.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "transactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_transaction_request_id",
            columnNames = "request_id"
        )
    },
    indexes = {
        @Index(
            name = "idx_transaction_status_due",
            columnList = "status, settlement_due_at"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne( optional = false)
    @JoinColumn(nullable = false)
    private Wallet senderWallet;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private Wallet receiverWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "settlement_due_at", nullable = false)
    private Instant settlementDueAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}