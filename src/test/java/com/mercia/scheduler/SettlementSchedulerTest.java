package com.mercia.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.service.SettlementService;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

	@Mock
	private SettlementService settlementService;

	@InjectMocks
	private SettlementScheduler scheduler;

	@Test
	void runSettlementDelegatesToSettlementService() {
		scheduler.runSettlement();

		verify(settlementService).settleDueTransactions();
	}
}
