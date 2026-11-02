package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.LedgerEntryReason;
import com.mercia.enums.LedgerEntryType;
import com.mercia.enums.TransactionStatus;

public record LedgerHistoryEntryResponse(
		Long id,
		LedgerEntryType entryType,
		LedgerEntryReason reason,
		BigDecimal amount,
		Instant createdAt,
		String description,
		String transferType,
		TransactionStatus status,
		Instant settlementDueAt) {
}
