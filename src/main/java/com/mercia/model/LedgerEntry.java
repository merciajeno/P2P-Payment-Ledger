package com.mercia.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_ledger_wallet_time",
               columnList = "wallet_id, created_at"),
        @Index(name = "idx_ledger_transaction",
               columnList = "transaction_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryReason reason;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 250)
    private String description;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}