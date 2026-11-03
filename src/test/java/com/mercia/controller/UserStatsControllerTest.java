package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import com.mercia.dto.UserStatsResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.service.UserStatsService;

@ExtendWith(MockitoExtension.class)
class UserStatsControllerTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserStatsService userStatsService;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private UserStatsController controller;

	@Test
	void getStatsReturnsStatsForAuthenticatedUsersWallet() {
		User user = new User();
		Wallet wallet = new Wallet();
		wallet.setId(42L);
		user.setWallet(wallet);
		UserStatsResponse stats = new UserStatsResponse(
				new BigDecimal("125.50"), new BigDecimal("80.00"), 7);
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));
		when(userStatsService.getStats(42L)).thenReturn(stats);

		var response = controller.getStats(authentication);

		assertEquals(200, response.getStatusCode().value());
		assertSame(stats, response.getBody());
		verify(userStatsService).getStats(42L);
	}

	@Test
	void getStatsThrowsWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.getStats(authentication));
	}

	@Test
	void getStatsThrowsWhenUserHasNoWallet() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(new User()));

		assertThrows(IllegalStateException.class, () -> controller.getStats(authentication));
	}
}
