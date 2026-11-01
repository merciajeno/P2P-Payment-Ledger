package com.mercia.dto;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        long totalUsers,
        long todayTransfersInitiated,
        long todayTransfersSettled,
        BigDecimal todayTransactionVolume,
        BigDecimal totalPendingSettlementAmount,
        long pendingFraudAlertsCount
) {}
