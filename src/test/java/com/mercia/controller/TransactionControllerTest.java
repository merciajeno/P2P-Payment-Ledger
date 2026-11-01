package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.mercia.dto.AmountRequest;
import com.mercia.dto.TransactionRequest;
import com.mercia.enums.TransactionStatus;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

	@Mock
	private TransactionService transactionService;
	@Mock
	private UserRepository userRepo;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private TransactionController controller;

	@Test
	void makeTransactionUsesAuthenticatedEmailAndRequestFields() {
		User user = user("alice", "alice@example.com");
		TransactionRequest request = new TransactionRequest(
				"bob@example.com", new BigDecimal("25.50"), "request-1", "1234", "Lunch");
		Transaction transaction = new Transaction();
		transaction.setRequestId("request-1");
		transaction.setStatus(TransactionStatus.PENDING);
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(user));
		when(transactionService.makeTransactionToOther(
				"alice@example.com", "bob@example.com", new BigDecimal("25.50"),
				"request-1", "1234", "Lunch")).thenReturn(transaction);

		var response = controller.makeTransactionToOther(request, authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("request-1", response.getBody().requestId());
		assertEquals(TransactionStatus.PENDING, response.getBody().transactionStatus());
		verify(transactionService).makeTransactionToOther(
				"alice@example.com", "bob@example.com", new BigDecimal("25.50"),
				"request-1", "1234", "Lunch");
	}

	@Test
	void depositUsesAuthenticatedEmailAndAmount() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(user("alice", "alice@example.com")));
		AmountRequest request = new AmountRequest(new BigDecimal("50.00"));

		var response = controller.depositAmount(request, authentication);

		assertEquals("Deposited amount", response.getBody());
		verify(transactionService).depositMoney("alice@example.com", new BigDecimal("50.00"));
	}

	@Test
	void withdrawUsesAuthenticatedEmailAndAmount() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(user("alice", "alice@example.com")));
		AmountRequest request = new AmountRequest(new BigDecimal("12.00"));

		var response = controller.withdrawAmount(request, authentication);

		assertEquals("Amount withdrawn", response.getBody());
		verify(transactionService).withdrawMoney("alice@example.com", new BigDecimal("12.00"));
	}

	@Test
	void transactionActionsThrowWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepo.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> controller.depositAmount(new AmountRequest(BigDecimal.ONE), authentication));
	}

	private User user(String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		return user;
	}
}
