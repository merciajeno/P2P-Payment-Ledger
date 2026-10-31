package com.mercia.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.mercia.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final TransactionRepository transactionRepo;

    public void checkDuplicateTransfer(
            Long senderWalletId,
            Long receiverWalletId) {

        Instant twoMinutesAgo = Instant.now().minusSeconds(120);

        boolean duplicate =
                transactionRepo
                    .existsBySenderWalletIdAndReceiverWalletIdAndCreatedAtAfter(
                        senderWalletId,
                        receiverWalletId,
                        twoMinutesAgo
                    );

        if (duplicate) {
        	log.info("Duplicate transaction detected");
            throw new IllegalArgumentException(
                "Duplicate transfer detected. Please try again later."
            );
        }
    }
}
