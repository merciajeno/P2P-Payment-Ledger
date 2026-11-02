package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.FraudAlertType;
import com.mercia.enums.Role;
import com.mercia.model.FraudAlert;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FraudAlertSaveServiceTest {

	@Mock
	private FraudAlertRepository fraudAlertRepo;
	@Mock
	private UserRepository userRepo;
	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private FraudAlertSaveService fraudAlertSaveService;

	@Test
	void saveInCurrentTransactionPersistsAlertAndNotifiesEveryAdmin() {
		Wallet wallet = new Wallet();
		wallet.setId(8L);
		Transaction transaction = new Transaction();
		transaction.setId(91L);
		BigDecimal amount = new BigDecimal("8500.00");
		User firstAdmin = admin("first@example.com");
		User secondAdmin = admin("second@example.com");
		when(userRepo.findByRole(Role.ADMIN)).thenReturn(List.of(firstAdmin, secondAdmin));

		fraudAlertSaveService.saveInCurrentTransaction(
				wallet, transaction, FraudAlertType.HIGH_VALUE_TRANSACTION,
				amount, "High-value transfer");

		ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
		verify(fraudAlertRepo).save(captor.capture());
		FraudAlert savedAlert = captor.getValue();
		assertSame(wallet, savedAlert.getWallet());
		assertSame(transaction, savedAlert.getTransaction());
		assertEquals(FraudAlertType.HIGH_VALUE_TRANSACTION, savedAlert.getAlertType());
		assertEquals(FraudAlertStatus.PENDING, savedAlert.getStatus());
		assertEquals(amount, savedAlert.getAmount());
		assertEquals("High-value transfer", savedAlert.getDescription());
		verify(userRepo).findByRole(Role.ADMIN);
		verify(notificationService).send(firstAdmin,
				"Fraud alert raised: HIGH_VALUE_TRANSACTION — ₹8500.00 by wallet #8");
		verify(notificationService).send(secondAdmin,
				"Fraud alert raised: HIGH_VALUE_TRANSACTION — ₹8500.00 by wallet #8");
	}

	@Test
	void savePersistsAlertWithoutNotificationsWhenThereAreNoAdmins() {
		Wallet wallet = new Wallet();
		wallet.setId(9L);
		when(userRepo.findByRole(Role.ADMIN)).thenReturn(List.of());

		fraudAlertSaveService.save(wallet, null, FraudAlertType.DUPLICATE_TRANSACTION,
				new BigDecimal("7.50"), "Duplicate transfer");

		verify(fraudAlertRepo).save(org.mockito.ArgumentMatchers.any(FraudAlert.class));
		verify(notificationService, org.mockito.Mockito.never()).send(
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(String.class));
	}

	private User admin(String email) {
		User admin = new User();
		admin.setEmail(email);
		admin.setRole(Role.ADMIN);
		return admin;
	}
}
