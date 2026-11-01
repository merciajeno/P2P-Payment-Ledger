package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.mercia.dto.AdminDashboardResponse;
import com.mercia.dto.AdminTransactionResponse;
import com.mercia.dto.AdminUserResponse;
import com.mercia.dto.FraudAlertResponse;
import com.mercia.dto.SettlementBatchResponse;
import com.mercia.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

	@Mock
	private AdminService adminService;

	@InjectMocks
	private AdminController controller;

	@Test
	void getDashboardReturnsServiceResponse() {
		AdminDashboardResponse dashboard = new AdminDashboardResponse(10, 4, 3, null, null, 2);
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
		ResponseEntity<String> response = controller.freezeWallet(12L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Wallet frozen", response.getBody());
		verify(adminService).freezeWallet(12L);
	}

	@Test
	void unfreezeWalletDelegatesUserIdAndReturnsConfirmation() {
		ResponseEntity<String> response = controller.unfreezeWallet(13L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Wallet unfrozen", response.getBody());
		verify(adminService).unfreezeWallet(13L);
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
		ResponseEntity<String> response = controller.manualSettle(21L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Transaction settled", response.getBody());
		verify(adminService).manualSettle(21L);
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
		ResponseEntity<String> response = controller.blockTransaction(31L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Transaction blocked and sender refunded", response.getBody());
		verify(adminService).blockTransaction(31L);
	}

	@Test
	void dismissAlertDelegatesAlertIdAndReturnsConfirmation() {
		ResponseEntity<String> response = controller.dismissAlert(32L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Alert dismissed", response.getBody());
		verify(adminService).dismissAlert(32L);
	}

	@Test
	void getRecentJobsReturnsServiceResponse() {
		List<SettlementBatchResponse> jobs = List.of();
		when(adminService.getRecentJobs()).thenReturn(jobs);

		assertSame(jobs, controller.getRecentJobs().getBody());

		verify(adminService).getRecentJobs();
	}
}
