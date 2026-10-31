package com.mercia.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mercia.service.LoginAttemptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountUnlockScheduler {

    private final LoginAttemptService loginAttemptService;

    @Scheduled(fixedRate = 300000)
    public void unlockAccounts() {

        log.info("Account unlock job started");

        loginAttemptService.unlockExpiredAccounts();

        log.info("Account unlock job completed");
    }
}