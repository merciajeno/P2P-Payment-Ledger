package com.mercia.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "daily_wallet_summary",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wallet_summary_date",
            columnNames = {"wallet_id", "summary_date"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class DailyWalletSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(
        name = "total_incoming",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal totalIncoming = BigDecimal.ZERO;

    @Column(
        name = "total_outgoing",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal totalOutgoing = BigDecimal.ZERO;
}