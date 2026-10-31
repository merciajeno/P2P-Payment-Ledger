package com.mercia.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.LoginAttempt;
import com.mercia.model.User;
import com.mercia.repository.LoginAttemptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepo;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    // Record a failed login
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(User user) {
       log.debug("Failed login");
        LoginAttempt attempt = loginAttemptRepo
                .findByUserId(user.getId())
                .orElseGet(() -> {
                    LoginAttempt newAttempt = new LoginAttempt();
                    newAttempt.setUser(user);
                    return newAttempt;
                });

        // Do not count attempts while already locked
        if (attempt.isLocked()) {
            return;
        }

        attempt.setFailedAttempts(attempt.getFailedAttempts() + 1);
        attempt.setLastFailedAt(Instant.now());

        if (attempt.getFailedAttempts() >= MAX_ATTEMPTS) {

            Instant lockedUntil = Instant.now()
                    .plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);

            attempt.setLocked(true);
            attempt.setLockedUntil(lockedUntil);
        }

        loginAttemptRepo.save(attempt);
    }

    // Reset attempts after successful login
    @Transactional
    public void recordSuccessfulLogin(User user) {

        loginAttemptRepo.findByUserId(user.getId())
                .ifPresent(attempt -> {
                    attempt.setFailedAttempts(0);
                    attempt.setLastFailedAt(null);
                    attempt.setLocked(false);
                    attempt.setLockedUntil(null);

                    loginAttemptRepo.save(attempt);
                });
    }

    // Check whether the account is locked
    @Transactional(readOnly = true)
    public boolean isAccountLocked(User user) {

        return loginAttemptRepo.findByUserId(user.getId())
                .map(LoginAttempt::isLocked)
                .orElse(false);
    }
    
    @Transactional
    public void unlockExpiredAccounts() {

        Instant now = Instant.now();

        List<LoginAttempt> expiredAttempts =
            loginAttemptRepo
                .findByLockedTrueAndLockedUntilLessThanEqual(now);

        for (LoginAttempt attempt : expiredAttempts) {

            attempt.setLocked(false);
            attempt.setFailedAttempts(0);
            attempt.setLockedUntil(null);

            loginAttemptRepo.save(attempt);

            log.info("Account unlocked: {}",
                attempt.getUser().getEmail());
        }
    }
}