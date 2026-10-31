package com.mercia.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mercia.service.DailySummaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySummaryScheduler {

    private final DailySummaryService dailySummaryService;

    // Runs at 11 PM IST (= 17:30 UTC since IST is UTC+5:30)
   // @Scheduled(cron = "0 30 17 * * *", zone = "UTC")
    @Scheduled(fixedRate = 60000)
    public void runDailySummary() {
        log.info("Daily summary job started");
        try {
            dailySummaryService.generateDailySummary();
            log.info("Daily summary job completed successfully");
        } catch (Exception e) {
            log.error("Daily summary job failed", e);
        }
    }
}
