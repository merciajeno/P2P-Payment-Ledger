package com.mercia.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.service.DailySummaryService;

@ExtendWith(MockitoExtension.class)
class DailySummarySchedulerTest {

	@Mock
	private DailySummaryService dailySummaryService;

	@InjectMocks
	private DailySummaryScheduler scheduler;

	@Test
	void runDailySummaryDelegatesToSummaryService() {
		scheduler.runDailySummary();

		verify(dailySummaryService).generateDailySummary();
	}

	@Test
	void runDailySummaryLogsServiceFailureWithoutPropagatingIt() {
		doThrow(new IllegalStateException("summary unavailable"))
				.when(dailySummaryService).generateDailySummary();

		assertDoesNotThrow(() -> scheduler.runDailySummary());

		verify(dailySummaryService).generateDailySummary();
	}
}
