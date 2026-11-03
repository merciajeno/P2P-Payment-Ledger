package com.mercia.dto;

import java.math.BigDecimal;

public record UserStatsResponse(
		BigDecimal totalSent,
		BigDecimal totalReceived,
		long transactionsThisMonth) {
}
