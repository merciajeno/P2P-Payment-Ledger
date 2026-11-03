package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.enums.TransactionStatus;
import com.mercia.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private UserStatsService userStatsService;

	@Test
	void getStatsReturnsSuccessfulTransferTotalsAndCurrentMonthCount() {
		when(transactionRepository.sumAmountBySenderWalletIdAndStatus(42L, TransactionStatus.SUCCESS))
				.thenReturn(new BigDecimal("125.50"));
		when(transactionRepository.sumAmountByReceiverWalletIdAndStatus(42L, TransactionStatus.SUCCESS))
				.thenReturn(new BigDecimal("80.00"));
		when(transactionRepository.countByWalletIdAndCreatedAtBetween(
				eq(42L), any(Instant.class), any(Instant.class))).thenReturn(7L);

		var stats = userStatsService.getStats(42L);

		assertEquals(new BigDecimal("125.50"), stats.totalSent());
		assertEquals(new BigDecimal("80.00"), stats.totalReceived());
		assertEquals(7L, stats.transactionsThisMonth());
		verify(transactionRepository).sumAmountBySenderWalletIdAndStatus(42L, TransactionStatus.SUCCESS);
		verify(transactionRepository).sumAmountByReceiverWalletIdAndStatus(42L, TransactionStatus.SUCCESS);
		verify(transactionRepository).countByWalletIdAndCreatedAtBetween(
				eq(42L), any(Instant.class), any(Instant.class));
	}
}
