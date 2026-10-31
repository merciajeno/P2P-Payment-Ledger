package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.mercia.model.FraudAlertType;
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

//    public void checkHighValue(Wallet senderWallet, Transaction transaction) {
//
//        BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("8000");
//
//        if (transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
//            saveAlert(senderWallet, transaction, FraudAlertType.HIGH_VALUE_TRANSACTION,
//                    transaction.getAmount(), "High-value transaction flagged for admin review");
//
//            log.info("High-value alert created for transaction {}", transaction.getId());
//        }
//    }

   
}


