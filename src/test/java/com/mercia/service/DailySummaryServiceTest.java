package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.model.DailySummary;
import com.mercia.model.Role;
import com.mercia.model.TransactionStatus;
import com.mercia.model.User;
import com.mercia.repository.DailySummaryRepository;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DailySummaryServiceTest {

	private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

	@Mock
	private DailySummaryRepository dailySummaryRepo;
	@Mock
	private UserRepository userRepo;
	@Mock
	private NotificationService notificationService;
	@Mock
	private FraudAlertRepository fraudAlertRepo;

	@InjectMocks
	private DailySummaryService dailySummaryService;

	@Test
	void generateDailySummarySkipsWhenSummaryAlreadyExists() {
		LocalDate today = LocalDate.now(INDIA_ZONE);
		when(dailySummaryRepo.findBySummaryDate(today)).thenReturn(Optional.of(new DailySummary()));

		dailySummaryService.generateDailySummary();

		verify(dailySummaryRepo, never()).save(any(DailySummary.class));
		verify(dailySummaryRepo, never()).countByStatusAndCreatedAtBetween(
				any(TransactionStatus.class), any(Instant.class), any(Instant.class));
		verify(userRepo, never()).findByRole(any(Role.class));
	}

	@Test
	void generateDailySummaryPersistsTotalsAndSuccessfulReconciliation() {
		stubNoSummary();
		stubDailyMetrics();
		when(dailySummaryRepo.sumSettledDebits(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("25.50"));
		when(dailySummaryRepo.sumSettledCredits(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("25.50"));

		dailySummaryService.generateDailySummary();

		DailySummary summary = savedSummary();
		assertEquals(LocalDate.now(INDIA_ZONE), summary.getSummaryDate());
		assertEquals(10, summary.getTransfersInitiated());
		assertEquals(new BigDecimal("80.00"), summary.getTotalAmountInitiated());
		assertEquals(2, summary.getTransfersSettled());
		assertEquals(new BigDecimal("25.50"), summary.getTotalAmountSettled());
		assertEquals(6, summary.getNewUserRegistrations());
		assertEquals(9, summary.getFraudAlertsRaised());
		assertEquals(new BigDecimal("15.00"), summary.getTotalPendingSettlementAmount());
		assertEquals(new BigDecimal("25.50"), summary.getTotalDebits());
		assertEquals(new BigDecimal("25.50"), summary.getTotalCredits());
		assertTrue(summary.getReconciliationPassed());

		ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(dailySummaryRepo).sumAmountInitiatedBetween(startCaptor.capture(), endCaptor.capture());
		Instant expectedStart = LocalDate.now(INDIA_ZONE).atStartOfDay(INDIA_ZONE).toInstant();
		Instant expectedEnd = LocalDate.now(INDIA_ZONE).plusDays(1).atStartOfDay(INDIA_ZONE).toInstant();
		assertEquals(expectedStart, startCaptor.getValue());
		assertEquals(expectedEnd, endCaptor.getValue());
		verify(userRepo, never()).findByRole(Role.ADMIN);
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void generateDailySummaryNotifiesAdminsWhenReconciliationFails() {
		stubNoSummary();
		stubDailyMetrics();
		when(dailySummaryRepo.sumSettledDebits(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("30.00"));
		when(dailySummaryRepo.sumSettledCredits(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("20.00"));
		User adminOne = admin("admin1@example.com");
		User adminTwo = admin("admin2@example.com");
		when(userRepo.findByRole(Role.ADMIN)).thenReturn(List.of(adminOne, adminTwo));

		dailySummaryService.generateDailySummary();

		DailySummary summary = savedSummary();
		assertFalse(summary.getReconciliationPassed());
		assertEquals(new BigDecimal("30.00"), summary.getTotalDebits());
		assertEquals(new BigDecimal("20.00"), summary.getTotalCredits());
		String expectedMessage = "CRITICAL: Reconciliation mismatch on " + LocalDate.now(INDIA_ZONE)
				+ ". Debits: ₹30.00, Credits: ₹20.00";
		verify(notificationService).send(adminOne, expectedMessage);
		verify(notificationService).send(adminTwo, expectedMessage);
	}

	private void stubNoSummary() {
		when(dailySummaryRepo.findBySummaryDate(LocalDate.now(INDIA_ZONE))).thenReturn(Optional.empty());
	}

	private void stubDailyMetrics() {
		when(dailySummaryRepo.countByStatusAndCreatedAtBetween(
				eq(TransactionStatus.PENDING), any(Instant.class), any(Instant.class))).thenReturn(1L);
		when(dailySummaryRepo.countByStatusAndCreatedAtBetween(
				eq(TransactionStatus.SUCCESS), any(Instant.class), any(Instant.class))).thenReturn(2L);
		when(dailySummaryRepo.countByStatusAndCreatedAtBetween(
				eq(TransactionStatus.FAILED), any(Instant.class), any(Instant.class))).thenReturn(3L);
		when(dailySummaryRepo.countByStatusAndCreatedAtBetween(
				eq(TransactionStatus.DECLINED), any(Instant.class), any(Instant.class))).thenReturn(4L);
		when(dailySummaryRepo.sumAmountInitiatedBetween(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("80.00"));
		when(dailySummaryRepo.sumAmountSettledBetween(
				eq(TransactionStatus.SUCCESS), any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("25.50"));
		when(dailySummaryRepo.countUsersRegisteredBetween(any(Instant.class), any(Instant.class))).thenReturn(6L);
		when(dailySummaryRepo.sumTotalPendingAmount()).thenReturn(new BigDecimal("15.00"));
		when(fraudAlertRepo.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(9L);
	}

	private DailySummary savedSummary() {
		ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
		verify(dailySummaryRepo).save(captor.capture());
		return captor.getValue();
	}

	private User admin(String email) {
		User admin = new User();
		admin.setEmail(email);
		admin.setRole(Role.ADMIN);
		return admin;
	}
}
