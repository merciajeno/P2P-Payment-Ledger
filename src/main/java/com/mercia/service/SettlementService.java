package com.mercia.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;
import com.mercia.model.Wallet;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TransactionRepository transactionRepo;
    private final WalletRepository walletRepo;

    public void settleDueTransactions() {

        List<Transaction> transactions =
                transactionRepo
                    .findByStatusAndSettlementDueAtLessThanEqual(
                        TransactionStatus.PENDING,
                        Instant.now()
                    );

        for (Transaction transaction : transactions) {
            settleOneTransaction(transaction.getId());
        }
    }

    @Transactional
    public void settleOneTransaction(Long transactionId) {

        Transaction transaction = transactionRepo
                .findById(transactionId)
                .orElseThrow(() ->
                    new IllegalStateException("Transaction not found"));

        // A second scheduler run must not settle it again.
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return;
        }

        if (transaction.getSettlementDueAt().isAfter(Instant.now())) {
            return;
        }

        Wallet receiverWallet = transaction.getReceiverWallet();

        receiverWallet.setBalance(
                receiverWallet.getBalance().add(transaction.getAmount())
        );

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setSettledAt(Instant.now());

        walletRepo.save(receiverWallet);
        transactionRepo.save(transaction);
    }
}