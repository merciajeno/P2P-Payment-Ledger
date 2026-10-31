package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.DailySummary;
import com.mercia.model.Role;
import com.mercia.model.TransactionStatus;
import com.mercia.repository.DailySummaryRepository;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private final FraudAlertRepository fraudAlertRepo;  // add this


    @Transactional
    public void generateDailySummary() {

        LocalDate today = LocalDate.now(INDIA_ZONE);

        // Idempotency: if summary for today already exists, skip
        if (dailySummaryRepo.findBySummaryDate(today).isPresent()) {
            log.info("Daily summary for {} already exists, skipping", today);
            return;
        }

        // Build the day's time window in UTC (IST = UTC+5:30)
        Instant startOfDay = today
                .atStartOfDay(INDIA_ZONE)
                .toInstant();

        Instant endOfDay = today
                .plusDays(1)
                .atStartOfDay(INDIA_ZONE)
                .toInstant();

        log.info("Generating daily summary for {} (UTC window: {} to {})",
                today, startOfDay, endOfDay);

        // ── 1. Transfers initiated today ──────────────────────────────
        long transfersInitiated = dailySummaryRepo
                .countByStatusAndCreatedAtBetween(
                        TransactionStatus.PENDING, startOfDay, endOfDay)
                + dailySummaryRepo.countByStatusAndCreatedAtBetween(
                        TransactionStatus.SUCCESS, startOfDay, endOfDay)
                + dailySummaryRepo.countByStatusAndCreatedAtBetween(
                        TransactionStatus.FAILED, startOfDay, endOfDay)
                + dailySummaryRepo.countByStatusAndCreatedAtBetween(
                        TransactionStatus.DECLINED, startOfDay, endOfDay);

        BigDecimal totalAmountInitiated = dailySummaryRepo
                .sumAmountInitiatedBetween(startOfDay, endOfDay);

        // ── 2. Transfers settled today ────────────────────────────────
        long transfersSettled = dailySummaryRepo
                .countByStatusAndCreatedAtBetween(
                        TransactionStatus.SUCCESS, startOfDay, endOfDay);

        BigDecimal totalAmountSettled = dailySummaryRepo
                .sumAmountSettledBetween(
                        TransactionStatus.SUCCESS, startOfDay, endOfDay);

        // ── 3. New user registrations today ──────────────────────────
        long newUsers = dailySummaryRepo
                .countUsersRegisteredBetween(startOfDay, endOfDay);

        // ── 4. Total pending settlement amount right now ──────────────
        BigDecimal totalPending = dailySummaryRepo.sumTotalPendingAmount();

        // ── 5. Reconciliation ─────────────────────────────────────────
        // For settled transactions: sum of all DEBIT entries must equal
        // sum of all CREDIT (SETTLEMENT) entries. Pending excluded since
        // their CREDIT hasn't been created yet.
        BigDecimal totalDebits = dailySummaryRepo
                .sumSettledDebits(startOfDay, endOfDay);

        BigDecimal totalCredits = dailySummaryRepo
                .sumSettledCredits(startOfDay, endOfDay);

        boolean reconciliationPassed =
                totalDebits.compareTo(totalCredits) == 0;

        if (!reconciliationPassed) {
            log.error(
                "RECONCILIATION MISMATCH on {}: debits={} credits={}",
                today, totalDebits, totalCredits
            );
            userRepo.findByRole(Role.ADMIN).forEach(admin ->
            notificationService.send(admin,
                "CRITICAL: Reconciliation mismatch on " + today
                    + ". Debits: ₹" + totalDebits
                    + ", Credits: ₹" + totalCredits)
        );
        }

        // ── 6. Persist ────────────────────────────────────────────────
        DailySummary summary = new DailySummary();
        summary.setSummaryDate(today);
        summary.setTransfersInitiated((int) transfersInitiated);
        summary.setTotalAmountInitiated(totalAmountInitiated);
        summary.setTransfersSettled((int) transfersSettled);
        summary.setTotalAmountSettled(totalAmountSettled);
        
        long fraudAlertsRaised = fraudAlertRepo.countByCreatedAtBetween(startOfDay, endOfDay);
        summary.setFraudAlertsRaised((int) fraudAlertsRaised);

        summary.setNewUserRegistrations((int) newUsers);
        summary.setTotalPendingSettlementAmount(totalPending);
        summary.setTotalDebits(totalDebits);
        summary.setTotalCredits(totalCredits);
        summary.setReconciliationPassed(reconciliationPassed);

        dailySummaryRepo.save(summary);

        log.info(
            "Daily summary saved: initiated={}, settled={}, pending=₹{}, reconciled={}",
            transfersInitiated, transfersSettled, totalPending, reconciliationPassed
        );
    }
}
