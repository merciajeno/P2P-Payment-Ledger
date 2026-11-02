package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.mercia.dto.AdminDashboardResponse;
import com.mercia.dto.AdminTransactionResponse;
import com.mercia.dto.AdminUserResponse;
import com.mercia.dto.FraudAlertResponse;
import com.mercia.dto.SettlementBatchResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

	@Mock
	private AdminService adminService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private AdminController controller;

	@Test
	void getDashboardReturnsServiceResponse() {
		AdminDashboardResponse dashboard = new AdminDashboardResponse(
				10, 4, 3, null, null, 2, null, null, true);
		when(adminService.getDashboard()).thenReturn(dashboard);

		ResponseEntity<AdminDashboardResponse> response = controller.getDashboard();

		assertEquals(200, response.getStatusCode().value());
		assertSame(dashboard, response.getBody());
		verify(adminService).getDashboard();
	}

	@Test
	void getUsersReturnsServiceResponse() {
		List<AdminUserResponse> users = List.of();
		when(adminService.getAllUsers()).thenReturn(users);

		assertSame(users, controller.getUsers().getBody());

		verify(adminService).getAllUsers();
	}

	@Test
	void freezeWalletDelegatesUserIdAndReturnsConfirmation() {
		User admin = stubAdmin();
		ResponseEntity<String> response = controller.freezeWallet(12L, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Wallet frozen", response.getBody());
		verify(adminService).freezeWallet(12L, admin);
	}

	@Test
	void unfreezeWalletDelegatesUserIdAndReturnsConfirmation() {
		User admin = stubAdmin();
		ResponseEntity<String> response = controller.unfreezeWallet(13L, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Wallet unfrozen", response.getBody());
		verify(adminService).unfreezeWallet(13L, admin);
	}

	@Test
	void walletActionsRejectUnknownAdmin() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.freezeWallet(12L, authentication));
		verifyNoInteractions(adminService);
	}

	@Test
	void getTransactionsPassesRequestedPageToService() {
		Page<AdminTransactionResponse> transactions = Page.empty();
		when(adminService.getAllTransactions(3)).thenReturn(transactions);

		assertSame(transactions, controller.getTransactions(3).getBody());

		verify(adminService).getAllTransactions(3);
	}

	@Test
	void getPendingSettlementsReturnsServiceResponse() {
		List<AdminTransactionResponse> transactions = List.of();
		when(adminService.getPendingTransactions()).thenReturn(transactions);

		assertSame(transactions, controller.getPendingSettlements().getBody());

		verify(adminService).getPendingTransactions();
	}

	@Test
	void manualSettleDelegatesTransactionIdAndReturnsConfirmation() {
		User admin = stubAdmin();
		ResponseEntity<String> response = controller.manualSettle(21L, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Transaction settled", response.getBody());
		verify(adminService).manualSettle(21L, admin);
	}

	@Test
	void getFraudAlertsReturnsServiceResponse() {
		List<FraudAlertResponse> alerts = List.of();
		when(adminService.getPendingFraudAlerts()).thenReturn(alerts);

		assertSame(alerts, controller.getFraudAlerts().getBody());

		verify(adminService).getPendingFraudAlerts();
	}

	@Test
	void blockTransactionDelegatesAlertIdAndReturnsConfirmation() {
		User admin = stubAdmin();
		ResponseEntity<String> response = controller.blockTransaction(31L, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Transaction blocked and sender refunded", response.getBody());
		verify(adminService).blockTransaction(31L, admin);
	}

	@Test
	void dismissAlertDelegatesAlertIdAndReturnsConfirmation() {
		User admin = stubAdmin();
		ResponseEntity<String> response = controller.dismissAlert(32L, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Alert dismissed", response.getBody());
		verify(adminService).dismissAlert(32L, admin);
	}

	@Test
	void getRecentJobsReturnsServiceResponse() {
		List<SettlementBatchResponse> jobs = List.of();
		when(adminService.getRecentJobs()).thenReturn(jobs);

		assertSame(jobs, controller.getRecentJobs().getBody());

		verify(adminService).getRecentJobs();
	}

	private User stubAdmin() {
		User admin = new User();
		admin.setName("admin");
		when(authentication.getName()).thenReturn("admin");
		when(userRepository.findByName("admin")).thenReturn(Optional.of(admin));
		return admin;
	}
}
