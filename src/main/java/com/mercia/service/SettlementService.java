package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.LedgerEntryReason;
import com.mercia.model.LedgerEntryType;
import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TransactionRepository transactionRepo;
    private final WalletRepository walletRepo;
    private final LedgerService ledgerService;
    private final DailyWalletSummaryService dailyWalletSummaryService;

    @Transactional
    public void settleDueTransactions() {

        List<Transaction> transactions =
                transactionRepo
                    .findByStatusAndSettlementDueAtLessThanEqual(
                        TransactionStatus.PENDING,
                        Instant.now()
                    );

        for (Transaction transaction : transactions) {
            settleOneTransaction(transaction);
        }
    }

    @Transactional
    private void settleOneTransaction(Transaction transaction) {

        Wallet receiverWallet = transaction.getReceiverWallet();

        if (receiverWallet.getWalletStatus() != WalletStatus.ACTIVE) {
            declineAndRefund(transaction);
            return;
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

        // Record ledger entry
        ledgerService.recordEntry(
            receiverWallet,
            transaction,
            LedgerEntryType.CREDIT,
            LedgerEntryReason.SETTLEMENT,
            amount,
            "Transfer received"
        );
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