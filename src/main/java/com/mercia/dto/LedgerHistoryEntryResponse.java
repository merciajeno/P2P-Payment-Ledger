package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.model.LedgerEntryReason;
import com.mercia.model.LedgerEntryType;

public record LedgerHistoryEntryResponse(
		Long id,
		LedgerEntryType entryType,
		LedgerEntryReason reason,
		BigDecimal amount,
		Instant createdAt,
		String description) {
}
