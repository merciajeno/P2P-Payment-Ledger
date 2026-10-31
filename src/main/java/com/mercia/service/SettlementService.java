package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.BatchStatus;
import com.mercia.model.LedgerEntryReason;
import com.mercia.model.LedgerEntryType;
import com.mercia.model.SettlementBatch;
import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.SettlementBatchRepository;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final TransactionRepository transactionRepo;
    private final WalletRepository walletRepo;
    private final LedgerService ledgerService;
    private final DailyWalletSummaryService dailyWalletSummaryService;
    private final SettlementBatchRepository settlementBatchRepo;
    private final NotificationService notificationService;

    @Transactional
    public void settleDueTransactions() {

        // ── Open the batch record ──────────────────────────────────────
        SettlementBatch batch = new SettlementBatch();
        batch.setStatus(BatchStatus.RUNNING);
        settlementBatchRepo.save(batch);   // gets an ID immediately

        List<Transaction> transactions =
                transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
                    TransactionStatus.PENDING, Instant.now());

        batch.setTotalProcessed(transactions.size());

        for (Transaction transaction : transactions) {
            try {
                boolean declined = settleOneTransaction(transaction);
                if (declined) {
                    batch.setDeclinedCount(batch.getDeclinedCount() + 1);
                } else {
                    batch.setSettledCount(batch.getSettledCount() + 1);
                    batch.setTotalAmountSettled(
                        batch.getTotalAmountSettled().add(transaction.getAmount()));
                }
            } catch (Exception e) {
                batch.setFailedCount(batch.getFailedCount() + 1);
                log.error("Settlement failed for transaction {}", transaction.getId(), e);
                
                // Mark failed and refund sender directly here
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepo.save(transaction);

                Wallet senderWallet = transaction.getSenderWallet();
                senderWallet.setBalance(senderWallet.getBalance().add(transaction.getAmount()));
                
                notificationService.send(transaction.getSenderWallet().getUser(),
                	    "₹" + transaction.getAmount() + " transfer failed. Amount refunded to your wallet");
                walletRepo.save(senderWallet);
                
            }

        }

        // ── Close the batch record ─────────────────────────────────────
        batch.setCompletedAt(Instant.now());
        if (batch.getFailedCount() == 0) {
            batch.setStatus(BatchStatus.SUCCESS);
        } else if (batch.getSettledCount() > 0 || batch.getDeclinedCount() > 0) {
            batch.setStatus(BatchStatus.PARTIAL_SUCCESS);
        } else {
            batch.setStatus(BatchStatus.FAILED);
        }
        settlementBatchRepo.save(batch);

        log.info("Settlement batch complete: settled={}, failed={}, declined={}, amount={}",
            batch.getSettledCount(), batch.getFailedCount(),
            batch.getDeclinedCount(), batch.getTotalAmountSettled());
    }


    @Transactional
    private boolean settleOneTransaction(Transaction transaction) {

        Wallet receiverWallet = transaction.getReceiverWallet();

        if (receiverWallet.getWalletStatus() != WalletStatus.ACTIVE) {
            declineAndRefund(transaction);
            return true;
        }

     //   Wallet senderWallet = transaction.getSenderWallet();
        BigDecimal amount = transaction.getAmount();

        // Credit receiver
        receiverWallet.setBalance(
            receiverWallet.getBalance().add(amount)
        );
        
     // Update daily incoming
        dailyWalletSummaryService.recordIncoming(
            receiverWallet,
            amount
        );
        walletRepo.save(receiverWallet);

        // Update transaction
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setSettledAt(Instant.now());
        transactionRepo.save(transaction);
        
        //notification
        notificationService.send(transaction.getSenderWallet().getUser(),
        	    "₹" + amount + " transfer to "
        	        + transaction.getReceiverWallet().getUser().getName() + " has been settled");

        	notificationService.send(transaction.getReceiverWallet().getUser(),
        	    "₹" + amount + " received from "
        	        + transaction.getSenderWallet().getUser().getName());


        // Record ledger entry
        ledgerService.recordEntry(
            receiverWallet,
            transaction,
            LedgerEntryType.CREDIT,
            LedgerEntryReason.SETTLEMENT,
            amount,
            "Transfer received"
        );
        return false;
    }
    
    @Transactional
    private void declineAndRefund(Transaction transaction) {

        Wallet senderWallet = transaction.getSenderWallet();
        BigDecimal amount = transaction.getAmount();

        // Refund sender
        senderWallet.setBalance(
            senderWallet.getBalance().add(amount)
        );
        
        //update it
        dailyWalletSummaryService.recordIncoming(
        	    senderWallet,
        	    amount
        	);
        walletRepo.save(senderWallet);

        // Update transaction
        transaction.setStatus(TransactionStatus.DECLINED);
        transactionRepo.save(transaction);

        //notification
        notificationService.send(transaction.getSenderWallet().getUser(),
        	    "₹" + amount + " transfer was declined. Amount refunded to your wallet");
        
        // Record refund in ledger
        ledgerService.recordEntry(
            senderWallet,
            transaction,
            LedgerEntryType.CREDIT,
            LedgerEntryReason.REFUND,
            amount,
            "Transfer refund - receiver wallet is inactive"
        );
    }
}