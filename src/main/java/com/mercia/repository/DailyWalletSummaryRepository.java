package com.mercia.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.DailyWalletSummary;

public interface DailyWalletSummaryRepository
        extends JpaRepository<DailyWalletSummary, Long> {

    Optional<DailyWalletSummary> findByWalletIdAndSummaryDate(
        Long walletId,
        LocalDate summaryDate
    );
}