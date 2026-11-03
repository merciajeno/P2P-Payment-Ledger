package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.mercia.enums.FraudAlertType;
import com.mercia.model.Transaction;
import com.mercia.model.Wallet;
import com.mercia.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final TransactionRepository transactionRepo;
    private final FraudAlertSaveService fraudAlertSaveService;
    private final NotificationService notificationService;
   
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("8000");

    public void checkDuplicateTransfer(
            Long senderWalletId,
            Long receiverWalletId,
            Wallet senderWallet,
            BigDecimal amount) {

        Instant twoMinutesAgo = Instant.now().minusSeconds(120);

        boolean duplicate =
                transactionRepo.existsBySenderWalletIdAndReceiverWalletIdAndCreatedAtAfter(
                    senderWalletId, receiverWalletId, twoMinutesAgo);

        if (duplicate) {
            fraudAlertSaveService.save(
                senderWallet,
                null,
                FraudAlertType.DUPLICATE_TRANSACTION,
                amount,
                "Duplicate transfer detected within 2 minutes"
            );

            log.warn("Duplicate transaction blocked for wallet {}", senderWalletId);
            
            notificationService.send(senderWallet.getUser(),
            	    "Transaction blocked: duplicate transfer detected");
            throw new IllegalArgumentException(
                "Duplicate transfer detected. Please try again later.");
        }
    }

 // Called AFTER transaction is saved — so transaction has an ID to link to
    // Does NOT block — transaction proceeds to PENDING, just raises an alert
    public void checkHighValue(Wallet senderWallet, Transaction transaction) {
     checkHighValue(senderWallet, transaction, false);
 }

 public void checkHighValue(Wallet senderWallet, Transaction transaction, boolean friendTransfer) {
     if (!friendTransfer && transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            fraudAlertSaveService.saveInCurrentTransaction(
                senderWallet,
                transaction,
                FraudAlertType.HIGH_VALUE_TRANSACTION,
                transaction.getAmount(),
                "High-value transaction flagged for admin review: ₹"
                    + transaction.getAmount()
            );
            log.info("High-value alert created for transaction {}", transaction.getId());
            // No throw — transaction continues to PENDING normally
            
        }
    }
   
}
