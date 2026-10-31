package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.model.LoginAttempt;
import com.mercia.model.User;
import com.mercia.repository.LoginAttemptRepository;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

	@Mock
	private LoginAttemptRepository loginAttemptRepo;
	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private LoginAttemptService loginAttemptService;

	@Test
	void recordFailedAttemptCreatesAndSavesFirstAttempt() {
		User user = user(11L);
		when(loginAttemptRepo.findByUserId(11L)).thenReturn(Optional.empty());

		loginAttemptService.recordFailedAttempt(user);

		ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
		verify(loginAttemptRepo).save(captor.capture());
		LoginAttempt saved = captor.getValue();
		assertEquals(user, saved.getUser());
		assertEquals(1, saved.getFailedAttempts());
		assertNotNull(saved.getLastFailedAt());
		assertFalse(saved.isLocked());
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void recordFailedAttemptLocksAccountAndNotifiesAtFifthFailure() {
		User user = user(12L);
		LoginAttempt attempt = attempt(user, 4, false);
		Instant before = Instant.now();
		when(loginAttemptRepo.findByUserId(12L)).thenReturn(Optional.of(attempt));

		loginAttemptService.recordFailedAttempt(user);

		assertEquals(5, attempt.getFailedAttempts());
		assertTrue(attempt.isLocked());
		assertNotNull(attempt.getLastFailedAt());
		assertNotNull(attempt.getLockedUntil());
		assertTrue(Duration.between(before, attempt.getLockedUntil()).toMinutes() >= 29);
		assertTrue(Duration.between(before, attempt.getLockedUntil()).toMinutes() <= 30);
		verify(loginAttemptRepo).save(attempt);
		verify(notificationService).send(user, "Your account has been automatically unlocked");
	}

	@Test
	void recordFailedAttemptDoesNotChangeAlreadyLockedAttempt() {
		User user = user(13L);
		LoginAttempt attempt = attempt(user, 5, true);
		when(loginAttemptRepo.findByUserId(13L)).thenReturn(Optional.of(attempt));

		loginAttemptService.recordFailedAttempt(user);

		assertEquals(5, attempt.getFailedAttempts());
		verify(loginAttemptRepo, never()).save(any(LoginAttempt.class));
		verify(notificationService, never()).send(any(), any(String.class));
	}

	@Test
	void recordSuccessfulLoginResetsAndSavesExistingAttempt() {
		User user = user(14L);
		LoginAttempt attempt = attempt(user, 5, true);
		attempt.setLastFailedAt(Instant.now());
		attempt.setLockedUntil(Instant.now().plusSeconds(600));
		when(loginAttemptRepo.findByUserId(14L)).thenReturn(Optional.of(attempt));

		loginAttemptService.recordSuccessfulLogin(user);

		assertEquals(0, attempt.getFailedAttempts());
		assertNull(attempt.getLastFailedAt());
		assertFalse(attempt.isLocked());
		assertNull(attempt.getLockedUntil());
		verify(loginAttemptRepo).save(attempt);
	}

	@Test
	void recordSuccessfulLoginDoesNothingWithoutExistingAttempt() {
		User user = user(15L);
		when(loginAttemptRepo.findByUserId(15L)).thenReturn(Optional.empty());

		loginAttemptService.recordSuccessfulLogin(user);

		verify(loginAttemptRepo, never()).save(any(LoginAttempt.class));
	}

	@Test
	void isAccountLockedReflectsStoredStateAndDefaultsToUnlocked() {
		User user = user(16L);
		when(loginAttemptRepo.findByUserId(16L)).thenReturn(Optional.of(attempt(user, 1, true)));
		assertTrue(loginAttemptService.isAccountLocked(user));

		when(loginAttemptRepo.findByUserId(16L)).thenReturn(Optional.of(attempt(user, 1, false)));
		assertFalse(loginAttemptService.isAccountLocked(user));

		when(loginAttemptRepo.findByUserId(16L)).thenReturn(Optional.empty());
		assertFalse(loginAttemptService.isAccountLocked(user));
	}

	@Test
	void unlockExpiredAccountsResetsEveryExpiredAttemptAndNotifiesUsers() {
		User firstUser = user(17L);
		User secondUser = user(18L);
		LoginAttempt first = attempt(firstUser, 5, true);
		LoginAttempt second = attempt(secondUser, 7, true);
		first.setLockedUntil(Instant.now().minusSeconds(1));
		second.setLockedUntil(Instant.now().minusSeconds(1));
		when(loginAttemptRepo.findByLockedTrueAndLockedUntilLessThanEqual(any(Instant.class)))
				.thenReturn(List.of(first, second));

		loginAttemptService.unlockExpiredAccounts();

		for (LoginAttempt attempt : List.of(first, second)) {
			assertFalse(attempt.isLocked());
			assertEquals(0, attempt.getFailedAttempts());
			assertNull(attempt.getLockedUntil());
			verify(loginAttemptRepo).save(attempt);
			verify(notificationService).send(attempt.getUser(),
					"Your account has been automatically unlocked");
		}
	}

	@Test
	void unlockExpiredAccountsDoesNothingWhenNoAttemptsAreExpired() {
		when(loginAttemptRepo.findByLockedTrueAndLockedUntilLessThanEqual(any(Instant.class)))
				.thenReturn(List.of());

		loginAttemptService.unlockExpiredAccounts();

		verify(loginAttemptRepo, never()).save(any(LoginAttempt.class));
		verify(notificationService, never()).send(any(), any(String.class));
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setEmail("user" + id + "@example.com");
		return user;
	}

	private LoginAttempt attempt(User user, int failedAttempts, boolean locked) {
		LoginAttempt attempt = new LoginAttempt();
		attempt.setUser(user);
		attempt.setFailedAttempts(failedAttempts);
		attempt.setLocked(locked);
		return attempt;
	}
}
