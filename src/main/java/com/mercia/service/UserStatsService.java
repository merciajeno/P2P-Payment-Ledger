package com.mercia.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.mercia.dto.UserStatsResponse;
import com.mercia.enums.TransactionStatus;
import com.mercia.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserStatsService {

	private static final ZoneId STATS_ZONE = ZoneId.of("Asia/Kolkata");

	private final TransactionRepository transactionRepository;

	public UserStatsResponse getStats(Long walletId) {
		LocalDate firstDayOfMonth = LocalDate.now(STATS_ZONE).withDayOfMonth(1);
		Instant monthStart = firstDayOfMonth.atStartOfDay(STATS_ZONE).toInstant();
		Instant nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay(STATS_ZONE).toInstant();

		return new UserStatsResponse(
				transactionRepository.sumAmountBySenderWalletIdAndStatus(
						walletId, TransactionStatus.SUCCESS),
				transactionRepository.sumAmountByReceiverWalletIdAndStatus(
						walletId, TransactionStatus.SUCCESS),
				transactionRepository.countByWalletIdAndCreatedAtBetween(
						walletId, monthStart, nextMonthStart));
	}
}
