package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.enums.FraudAlertType;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

	@Mock
	private TransactionRepository transactionRepo;
	@Mock
	private FraudAlertSaveService fraudAlertSaveService;
	@Mock
	private NotificationService notificationService;

	@Test
	void checkDuplicateTransferDoesNothingWhenNoRecentTransferExists() {
		FraudDetectionService service = new FraudDetectionService(
				transactionRepo, fraudAlertSaveService, notificationService);
		Wallet wallet = wallet();
		when(transactionRepo.existsBySenderWalletIdAndReceiverWalletIdAndCreatedAtAfter(
				eq(1L), eq(2L), any())).thenReturn(false);

		service.checkDuplicateTransfer(1L, 2L, wallet, new BigDecimal("5.00"));

		verify(fraudAlertSaveService, never()).save(any(), any(), any(), any(), any());
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void checkDuplicateTransferSavesAlertNotifiesAndRejectsDuplicate() {
		FraudDetectionService service = new FraudDetectionService(
				transactionRepo, fraudAlertSaveService, notificationService);
		Wallet wallet = wallet();
		BigDecimal amount = new BigDecimal("5.00");
		when(transactionRepo.existsBySenderWalletIdAndReceiverWalletIdAndCreatedAtAfter(
				eq(1L), eq(2L), any())).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> service.checkDuplicateTransfer(1L, 2L, wallet, amount));

		verify(fraudAlertSaveService).save(wallet, null, FraudAlertType.DUPLICATE_TRANSACTION,
				amount, "Duplicate transfer detected within 2 minutes");
		verify(notificationService).send(wallet.getUser(),
				"Transaction blocked: duplicate transfer detected");
	}

	@Test
	void checkHighValueCreatesAlertOnlyAboveThreshold() {
		FraudDetectionService service = new FraudDetectionService(
				transactionRepo, fraudAlertSaveService, notificationService);
		Wallet wallet = wallet();
		Transaction highValue = new Transaction();
		highValue.setId(56L);
		highValue.setAmount(new BigDecimal("8000.01"));
		service.checkHighValue(wallet, highValue);

		verify(fraudAlertSaveService).saveInCurrentTransaction(
				wallet, highValue, FraudAlertType.HIGH_VALUE_TRANSACTION,
				highValue.getAmount(),
				"High-value transaction flagged for admin review: ₹8000.01");
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void checkHighValueDoesNotCreateAlertAtOrBelowThreshold() {
		FraudDetectionService service = new FraudDetectionService(
				transactionRepo, fraudAlertSaveService, notificationService);
		Wallet wallet = wallet();
		Transaction atThreshold = new Transaction();
		atThreshold.setAmount(new BigDecimal("8000.00"));
		Transaction belowThreshold = new Transaction();
		belowThreshold.setAmount(new BigDecimal("7999.99"));

		service.checkHighValue(wallet, atThreshold);
		service.checkHighValue(wallet, belowThreshold);

		verify(fraudAlertSaveService, never()).save(any(), any(), any(), any(), any());
	}

	@Test
	void checkHighValueDoesNotCreateAlertForFriendTransfer() {
		FraudDetectionService service = new FraudDetectionService(
				transactionRepo, fraudAlertSaveService, notificationService);
		Wallet wallet = wallet();
		Transaction highValue = new Transaction();
		highValue.setAmount(new BigDecimal("8000.01"));

		service.checkHighValue(wallet, highValue, true);

		verify(fraudAlertSaveService, never())
				.saveInCurrentTransaction(any(), any(), any(), any(), any());
	}

	private Wallet wallet() {
		User user = new User();
		user.setEmail("sender@example.com");
		Wallet wallet = new Wallet();
		wallet.setId(1L);
		wallet.setUser(user);
		return wallet;
	}
}
