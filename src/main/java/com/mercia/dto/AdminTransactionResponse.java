package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.TransactionStatus;

public record AdminTransactionResponse(
        Long id,
        String senderName,
        String receiverName,
        BigDecimal amount,
        TransactionStatus status,
        String remark,
        Instant createdAt,
        Instant settlementDueAt,
        Instant settledAt
) {}
