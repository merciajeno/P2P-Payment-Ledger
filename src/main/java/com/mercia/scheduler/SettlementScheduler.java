package com.mercia.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mercia.service.SettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final SettlementService settlementService;

    @Scheduled(fixedRate = 60000)
    public void runSettlement() {

        log.info("Settlement job started");

        settlementService.settleDueTransactions();

        log.info("Settlement job finished");
    }
}