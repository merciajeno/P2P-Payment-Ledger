package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.dto.AdminDashboardResponse;
import com.mercia.dto.AdminTransactionResponse;
import com.mercia.dto.AdminUserResponse;
import com.mercia.dto.FraudAlertResponse;
import com.mercia.dto.SettlementBatchResponse;
import com.mercia.enums.AdminAction;
import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.TransactionStatus;
import com.mercia.enums.WalletStatus;
import com.mercia.model.AuditLog;
import com.mercia.model.FraudAlert;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.AuditLogRepository;
import com.mercia.repository.DailySummaryRepository;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.SettlementBatchRepository;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;
    private final DailySummaryRepository dailySummaryRepo;
    private final FraudAlertRepository fraudAlertRepo;
    private final SettlementBatchRepository settlementBatchRepo;
    private final SettlementService settlementService;
    private final NotificationService notificationService;
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private final AuditLogRepository auditLogRepo;

    // audit log
    @Transactional
    private void audit(User admin, AdminAction action,
            Long targetUserId, Long targetEntityId, String description) {
        AuditLog log = new AuditLog();
        log.setAdmin(admin);
        log.setAction(action);
        log.setTargetUserId(targetUserId);
        log.setTargetEntityId(targetEntityId);
        log.setDescription(description);
        auditLogRepo.save(log);
    }


    // ── Dashboard ──────────────────────────────────────────────────────

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(INDIA_ZONE);
        Instant startOfDay = today.atStartOfDay(INDIA_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(INDIA_ZONE).toInstant();

        long totalUsers = userRepo.count();

        long todayInitiated = transactionRepo
                .countByCreatedAtBetween(startOfDay, endOfDay);

        long todaySettled = transactionRepo
                .countByStatusAndSettledAtBetween(
                        TransactionStatus.SUCCESS, startOfDay, endOfDay);

        BigDecimal todayVolume = transactionRepo
                .sumAmountByCreatedAtBetween(startOfDay, endOfDay);

        BigDecimal totalPending = transactionRepo.sumPendingAmount();

        BigDecimal todaySettledDebits = dailySummaryRepo.sumSettledDebits(startOfDay, endOfDay);
        BigDecimal todaySettledCredits = dailySummaryRepo.sumSettledCredits(startOfDay, endOfDay);
        boolean reconciliationPassed = todaySettledDebits.compareTo(todaySettledCredits) == 0;

        long pendingFraudAlerts = fraudAlertRepo
                .countByStatus(FraudAlertStatus.PENDING);

        return new AdminDashboardResponse(
                totalUsers,
                todayInitiated,
                todaySettled,
                todayVolume,
                totalPending,
                pendingFraudAlerts,
                todaySettledDebits,
                todaySettledCredits,
                reconciliationPassed
        );
    }

    // ── User List ──────────────────────────────────────────────────────

    public List<AdminUserResponse> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public void freezeWallet(Long userId,User admin) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletStatus(WalletStatus.FROZEN);
        walletRepo.save(wallet);
        log.info("Admin froze wallet for user {}", userId);
        audit(admin, AdminAction.FREEZE_WALLET, userId, null,
                "Froze wallet for user " + wallet.getUser().getEmail());
    }

    @Transactional
    public void unfreezeWallet(Long userId,User admin) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletStatus(WalletStatus.ACTIVE);
        walletRepo.save(wallet);
        log.info("Admin unfroze wallet for user {}", userId);
        audit(admin, AdminAction.UNFREEZE_WALLET, userId, null,
                "Unfroze wallet for user " + wallet.getUser().getEmail());
    }

    // ── Transaction Monitoring ─────────────────────────────────────────

    public Page<AdminTransactionResponse> getAllTransactions(int page) {
        return transactionRepo
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20))
                .map(this::toTransactionResponse);
    }

    // ── Settlement Monitor ─────────────────────────────────────────────

    public List<AdminTransactionResponse> getPendingTransactions() {
        return transactionRepo
                .findByStatus(TransactionStatus.PENDING)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public void manualSettle(Long transactionId,User admin) {
        Transaction transaction = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be manually settled");
        }

        settlementService.settleOneTransaction(transaction);
        log.info("Admin manually settled transaction {}", transactionId);
        audit(admin, AdminAction.MANUAL_SETTLE, null, transactionId,
                "Manually settled transaction #" + transactionId);
    }

    // ── Fraud Review ───────────────────────────────────────────────────

    public List<FraudAlertResponse> getPendingFraudAlerts() {
        return fraudAlertRepo
                .findByStatusOrderByCreatedAtDesc(FraudAlertStatus.PENDING)
                .stream()
                .map(this::toFraudAlertResponse)
                .toList();
    }

    @Transactional
    public void blockTransaction(Long alertId,User admin) {
        FraudAlert alert = getFraudAlert(alertId);
        Transaction transaction = alert.getTransaction();

        if (transaction == null) {
            throw new IllegalStateException("No transaction linked to this alert");
        }
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is no longer pending");
        }

        // Refund sender
        Wallet senderWallet = transaction.getSenderWallet();
        senderWallet.setBalance(senderWallet.getBalance().add(transaction.getAmount()));
        walletRepo.save(senderWallet);

        // Fail the transaction
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepo.save(transaction);

        // Resolve alert
        alert.setStatus(FraudAlertStatus.BLOCKED);
        alert.setResolvedAt(Instant.now());
        fraudAlertRepo.save(alert);
        
     // notify sender
        notificationService.send(
            senderWallet.getUser(),
            "₹" + transaction.getAmount() + " transfer was blocked by admin. Amount refunded to your wallet"
        );


        log.info("Admin blocked transaction {} via alert {}", transaction.getId(), alertId);
        audit(admin, AdminAction.BLOCK_TRANSACTION,
                senderWallet.getUser().getId(), transaction.getId(),
                "Blocked transaction #" + transaction.getId() + " via alert #" + alertId);
    }

    @Transactional
    public void dismissAlert(Long alertId,User admin) {
        FraudAlert alert = getFraudAlert(alertId);
        alert.setStatus(FraudAlertStatus.DISMISSED);
        alert.setResolvedAt(Instant.now());
        fraudAlertRepo.save(alert);
        log.info("Admin dismissed alert {}", alertId);
        
        audit(admin, AdminAction.DISMISS_ALERT, null, alertId,
                "Dismissed fraud alert #" + alertId);
    }

    // ── Job Monitor ────────────────────────────────────────────────────

    public List<SettlementBatchResponse> getRecentJobs() {
        return settlementBatchRepo
                .findTop20ByOrderByStartedAtDesc()
                .stream()
                .map(b -> new SettlementBatchResponse(
                        b.getId(), b.getStartedAt(), b.getCompletedAt(),
                        b.getStatus(), b.getTotalProcessed(), b.getSettledCount(),
                        b.getFailedCount(), b.getDeclinedCount(),
                        b.getTotalAmountSettled()))
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private AdminUserResponse toUserResponse(User u) {
        Wallet w = u.getWallet();
        return new AdminUserResponse(
                u.getId(), u.getName(), u.getEmail(), u.getPhoneNumber(),
                w != null ? w.getBalance() : BigDecimal.ZERO,
                w != null ? w.getWalletStatus() : null,
                u.getCreatedAt()
        );
    }

    private AdminTransactionResponse toTransactionResponse(Transaction t) {
        return new AdminTransactionResponse(
                t.getId(),
                t.getSenderWallet().getUser().getName(),
                t.getReceiverWallet().getUser().getName(),
                t.getAmount(),
                t.getStatus(),
                t.getRemark(),
                t.getCreatedAt(),
                t.getSettlementDueAt(),
                t.getSettledAt()
        );
    }

    private FraudAlertResponse toFraudAlertResponse(FraudAlert a) {
        return new FraudAlertResponse(
                a.getId(), a.getAlertType(), a.getStatus(),
                a.getAmount(), a.getDescription(), a.getCreatedAt(),
                a.getTransaction() != null ? a.getTransaction().getId() : null,
                a.getWallet().getUser().getName()
        );
    }

    private Wallet getWalletByUserId(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = user.getWallet();
        if (wallet == null) throw new IllegalStateException("Wallet not found");
        return wallet;
    }

    private FraudAlert getFraudAlert(Long alertId) {
        return fraudAlertRepo.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
    }
}
