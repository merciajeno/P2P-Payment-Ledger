package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.FraudAlertType;

public record FraudAlertResponse(
        Long alertId,
        FraudAlertType alertType,
        FraudAlertStatus status,
        BigDecimal amount,
        String description,
        Instant createdAt,
        Long transactionId,
        String walletOwnerName
) {}
