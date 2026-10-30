package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.mercia.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepo;

    public void checkDuplicateTransfer(
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount) {

        Instant twoMinutesAgo = Instant.now().minusSeconds(120);

        boolean duplicate =
                transactionRepo
                    .existsBySenderWalletIdAndReceiverWalletIdAndAmountAndCreatedAtAfter(
                        senderWalletId,
                        receiverWalletId,
                        amount,
                        twoMinutesAgo
                    );

        if (duplicate) {
            throw new IllegalArgumentException(
                "Duplicate transfer detected. Please try again later."
            );
        }
    }
}
