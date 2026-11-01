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

import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.service.WalletService;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private WalletService walletService;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private WalletController controller;

	@Test
	void getBalanceReturnsAuthenticatedUsersWalletBalance() {
		User user = user("alice");
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("75.50"));
		user.setWallet(wallet);
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

		var response = controller.getBalance(authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(new BigDecimal("75.50"), response.getBody().balance());
	}

	@Test
	void getBalanceThrowsWhenWalletOrBalanceIsUnavailable() {
		User user = user("alice");
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));
		assertThrows(IllegalStateException.class, () -> controller.getBalance(authentication));

		Wallet wallet = new Wallet();
		user.setWallet(wallet);
		assertThrows(IllegalStateException.class, () -> controller.getBalance(authentication));
	}

	@Test
	void deactivateDelegatesAuthenticatedUserAndReturnsConfirmation() {
		User user = user("alice");
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

		var response = controller.deactivate(authentication);

		assertEquals("Wallet deactivated", response.getBody());
		verify(walletService).deactivate(user);
	}

	@Test
	void reactivateDelegatesAuthenticatedUserAndReturnsConfirmation() {
		User user = user("alice");
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

		var response = controller.reactivate(authentication);

		assertEquals("Wallet reactivated", response.getBody());
		verify(walletService).reactivate(user);
	}

	@Test
	void walletActionsThrowWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.reactivate(authentication));
	}

	private User user(String name) {
		User user = new User();
		user.setName(name);
		return user;
	}
}
