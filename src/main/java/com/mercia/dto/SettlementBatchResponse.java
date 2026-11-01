package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.BatchStatus;

public record SettlementBatchResponse(
        Long id,
        Instant startedAt,
        Instant completedAt,
        BatchStatus status,
        int totalProcessed,
        int settledCount,
        int failedCount,
        int declinedCount,
        BigDecimal totalAmountSettled
) {}
