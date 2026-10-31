package com.mercia.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.service.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class AccountUnlockSchedulerTest {

	@Mock
	private LoginAttemptService loginAttemptService;

	@InjectMocks
	private AccountUnlockScheduler scheduler;

	@Test
	void unlockAccountsDelegatesToLoginAttemptService() {
		scheduler.unlockAccounts();

		verify(loginAttemptService).unlockExpiredAccounts();
	}
}
