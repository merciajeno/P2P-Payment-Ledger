package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import com.mercia.dto.LedgerHistoryEntryResponse;
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
		when(ledgerService.getWalletHistory(42L, 2)).thenReturn(history);

		var response = controller.getWalletHistory(authentication, 2);

		assertEquals(200, response.getStatusCode().value());
		assertSame(history, response.getBody());
		verify(ledgerService).getWalletHistory(42L, 2);
	}

	@Test
	void getWalletHistoryThrowsWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepo.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.getWalletHistory(authentication, 0));
	}

	@Test
	void getWalletHistoryThrowsWhenUserHasNoWallet() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepo.findByName("alice")).thenReturn(Optional.of(new User()));

		assertThrows(IllegalStateException.class, () -> controller.getWalletHistory(authentication, 0));
	}
}
