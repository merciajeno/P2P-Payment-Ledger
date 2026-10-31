package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.model.DailyWalletSummary;
import com.mercia.model.FraudAlertType;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.DailyWalletSummaryRepository;

@ExtendWith(MockitoExtension.class)
class DailyWalletSummaryServiceTest {

	private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

	@Mock
	private DailyWalletSummaryRepository summaryRepo;
	@Mock
	private FraudAlertSaveService fraudAlertSaveService;
	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private DailyWalletSummaryService dailyWalletSummaryService;

	@Test
	void getOrCreateSummaryReturnsExistingWalletSummary() {
		Wallet wallet = wallet(31L);
		DailyWalletSummary existing = summary(wallet, "12.00", "18.00");
		when(summaryRepo.findByWalletIdAndSummaryDate(31L, LocalDate.now(INDIA_ZONE)))
				.thenReturn(Optional.of(existing));

		assertSame(existing, dailyWalletSummaryService.getOrCreateSummary(wallet));

		verify(summaryRepo, never()).save(any(DailyWalletSummary.class));
	}

	@Test
	void getOrCreateSummarySavesZeroedSummaryWhenMissing() {
		Wallet wallet = wallet(32L);
		when(summaryRepo.findByWalletIdAndSummaryDate(eq(32L), any(LocalDate.class)))
				.thenReturn(Optional.empty());
		when(summaryRepo.save(any(DailyWalletSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DailyWalletSummary created = dailyWalletSummaryService.getOrCreateSummary(wallet);

		assertSame(wallet, created.getWallet());
		assertEquals(LocalDate.now(INDIA_ZONE), created.getSummaryDate());
		assertEquals(BigDecimal.ZERO, created.getTotalIncoming());
		assertEquals(BigDecimal.ZERO, created.getTotalOutgoing());
		ArgumentCaptor<DailyWalletSummary> captor = ArgumentCaptor.forClass(DailyWalletSummary.class);
		verify(summaryRepo).save(captor.capture());
		assertSame(created, captor.getValue());
	}

	@Test
	void checkAndRecordOutgoingPersistsTotalAtDailyLimit() {
		Wallet wallet = wallet(33L);
		DailyWalletSummary existing = summary(wallet, "0.00", "40000.00");
		when(summaryRepo.findByWalletIdAndSummaryDate(eq(33L), any(LocalDate.class)))
				.thenReturn(Optional.of(existing));

		dailyWalletSummaryService.checkAndRecordOutgoing(wallet, new BigDecimal("10000.00"));

		assertEquals(new BigDecimal("50000.00"), existing.getTotalOutgoing());
		verify(summaryRepo).save(existing);
		verify(fraudAlertSaveService, never()).save(any(), any(), any(), any(), any());
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void checkAndRecordOutgoingRaisesAlertAndRejectsAmountAboveDailyLimit() {
		Wallet wallet = wallet(34L);
		DailyWalletSummary existing = summary(wallet, "0.00", "49999.00");
		when(summaryRepo.findByWalletIdAndSummaryDate(eq(34L), any(LocalDate.class)))
				.thenReturn(Optional.of(existing));
		BigDecimal amount = new BigDecimal("1.01");

		assertThrows(IllegalArgumentException.class,
				() -> dailyWalletSummaryService.checkAndRecordOutgoing(wallet, amount));

		verify(fraudAlertSaveService).save(wallet, null, FraudAlertType.DAILY_LIMIT_EXCEEDED,
				amount, "Daily outgoing limit of ₹50,000 exceeded");
		verify(notificationService).send(wallet.getUser(), "Transaction blocked: daily limit exceeded");
		verify(summaryRepo, never()).save(existing);
		assertEquals(new BigDecimal("49999.00"), existing.getTotalOutgoing());
	}

	@Test
	void recordIncomingIncrementsExistingIncomingTotal() {
		Wallet wallet = wallet(35L);
		DailyWalletSummary existing = summary(wallet, "50.00", "10.00");
		when(summaryRepo.findByWalletIdAndSummaryDate(eq(35L), any(LocalDate.class)))
				.thenReturn(Optional.of(existing));

		dailyWalletSummaryService.recordIncoming(wallet, new BigDecimal("15.25"));

		assertEquals(new BigDecimal("65.25"), existing.getTotalIncoming());
		verify(summaryRepo).save(existing);
	}

	@Test
	void recordIncomingCreatesSummaryWhenMissing() {
		Wallet wallet = wallet(36L);
		when(summaryRepo.findByWalletIdAndSummaryDate(eq(36L), any(LocalDate.class)))
				.thenReturn(Optional.empty());
		when(summaryRepo.save(any(DailyWalletSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		dailyWalletSummaryService.recordIncoming(wallet, new BigDecimal("12.50"));

		ArgumentCaptor<DailyWalletSummary> captor = ArgumentCaptor.forClass(DailyWalletSummary.class);
		verify(summaryRepo, org.mockito.Mockito.times(2)).save(captor.capture());
		DailyWalletSummary createdAndUpdated = captor.getAllValues().get(1);
		assertSame(wallet, createdAndUpdated.getWallet());
		assertEquals(new BigDecimal("12.50"), createdAndUpdated.getTotalIncoming());
		assertEquals(BigDecimal.ZERO, createdAndUpdated.getTotalOutgoing());
	}

	private Wallet wallet(Long id) {
		User user = new User();
		user.setEmail("wallet" + id + "@example.com");
		Wallet wallet = new Wallet();
		wallet.setId(id);
		wallet.setUser(user);
		return wallet;
	}

	private DailyWalletSummary summary(Wallet wallet, String incoming, String outgoing) {
		DailyWalletSummary summary = new DailyWalletSummary();
		summary.setWallet(wallet);
		summary.setSummaryDate(LocalDate.now(INDIA_ZONE));
		summary.setTotalIncoming(new BigDecimal(incoming));
		summary.setTotalOutgoing(new BigDecimal(outgoing));
		return summary;
	}
}
