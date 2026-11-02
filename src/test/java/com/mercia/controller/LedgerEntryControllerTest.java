package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.enums.LedgerHistoryType;
import com.mercia.enums.TransactionStatus;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.service.LedgerService;

@ExtendWith(MockitoExtension.class)
class LedgerEntryControllerTest {

	@Mock
	private LedgerService ledgerService;
	@Mock
	private UserRepository userRepo;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private LedgerEntryController controller;

	@Test
	void getWalletHistoryReturnsHistoryForAuthenticatedUsersWallet() {
		User user = new User();
		Wallet wallet = new Wallet();
		wallet.setId(42L);
		user.setWallet(wallet);
		Page<LedgerHistoryEntryResponse> history = Page.empty();
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(user));
		when(ledgerService.getWalletHistory(42L, 2, LedgerHistoryType.ALL, null, null, null)).thenReturn(history);

		var response = controller.getWalletHistory(authentication, 2, LedgerHistoryType.ALL, null, null, null);

		assertEquals(200, response.getStatusCode().value());
		assertSame(history, response.getBody());
		verify(ledgerService).getWalletHistory(42L, 2, LedgerHistoryType.ALL, null, null, null);
	}

	@Test
	void getWalletHistoryPassesAllFiltersToService() {
		User user = new User();
		Wallet wallet = new Wallet();
		wallet.setId(42L);
		user.setWallet(wallet);
		LocalDate from = LocalDate.parse("2026-01-01");
		LocalDate to = LocalDate.parse("2026-01-31");
		Page<LedgerHistoryEntryResponse> history = Page.empty();
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(user));
		when(ledgerService.getWalletHistory(
				42L, 3, LedgerHistoryType.RECEIVED, TransactionStatus.PENDING, from, to))
				.thenReturn(history);

		var response = controller.getWalletHistory(
				authentication, 3, LedgerHistoryType.RECEIVED, TransactionStatus.PENDING, from, to);

		assertEquals(200, response.getStatusCode().value());
		assertSame(history, response.getBody());
		verify(ledgerService).getWalletHistory(
				42L, 3, LedgerHistoryType.RECEIVED, TransactionStatus.PENDING, from, to);
	}

	@Test
	void getWalletHistoryRejectsReversedDateRangeBeforeLoadingUser() {
		LocalDate from = LocalDate.parse("2026-02-01");
		LocalDate to = LocalDate.parse("2026-01-31");

		var exception = assertThrows(ResponseStatusException.class,
				() -> controller.getWalletHistory(
						authentication, 0, LedgerHistoryType.ALL, null, from, to));

		assertEquals(400, exception.getStatusCode().value());
		verifyNoInteractions(userRepo, ledgerService);
	}

	@Test
	void getWalletHistoryThrowsWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepo.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> controller.getWalletHistory(authentication, 0, LedgerHistoryType.ALL, null, null, null));
	}

	@Test
	void getWalletHistoryThrowsWhenUserHasNoWallet() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(new User()));

		assertThrows(IllegalStateException.class,
				() -> controller.getWalletHistory(authentication, 0, LedgerHistoryType.ALL, null, null, null));
	}
}
