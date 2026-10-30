package com.mercia.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.DailyWalletSummary;
import com.mercia.model.Wallet;
import com.mercia.repository.DailyWalletSummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyWalletSummaryService {

    private final DailyWalletSummaryRepository summaryRepo;

    private static final BigDecimal DAILY_LIMIT =
            new BigDecimal("50000");

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private LocalDate getToday() {
        return LocalDate.now(INDIA_ZONE);
    }

    @Transactional
    public DailyWalletSummary getOrCreateSummary(Wallet wallet) {

        LocalDate today = getToday();

        return summaryRepo
                .findByWalletIdAndSummaryDate(wallet.getId(), today)
                .orElseGet(() -> {
                    DailyWalletSummary summary =
                            new DailyWalletSummary();

                    summary.setWallet(wallet);
                    summary.setSummaryDate(today);
                    summary.setTotalIncoming(BigDecimal.ZERO);
                    summary.setTotalOutgoing(BigDecimal.ZERO);

                    return summaryRepo.save(summary);
                });
    }

    @Transactional
    public void checkAndRecordOutgoing(
            Wallet wallet,
            BigDecimal amount) {

        DailyWalletSummary summary = getOrCreateSummary(wallet);

        BigDecimal newTotal =
                summary.getTotalOutgoing().add(amount);

        if (newTotal.compareTo(DAILY_LIMIT) > 0) {
            throw new IllegalArgumentException(
                    "Daily outgoing limit of ₹50,000 exceeded"
            );
        }

        summary.setTotalOutgoing(newTotal);
        summaryRepo.save(summary);
    }

    @Transactional
    public void recordIncoming(
            Wallet wallet,
            BigDecimal amount) {

        DailyWalletSummary summary = getOrCreateSummary(wallet);

        summary.setTotalIncoming(
                summary.getTotalIncoming().add(amount)
        );

        summaryRepo.save(summary);
    }
}