package com.mercia.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.enums.LedgerHistoryType;
import com.mercia.enums.TransactionStatus;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.service.LedgerService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/ledger")
public class LedgerEntryController {

    private final LedgerService ledgerService;
    private final UserRepository userRepo;

    @GetMapping("/history")
    public ResponseEntity<Page<LedgerHistoryEntryResponse>> getWalletHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "ALL") LedgerHistoryType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Start date must be on or before end date");
        }

        User user = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        Wallet wallet = user.getWallet();

        if (wallet == null) {
            throw new IllegalStateException("Wallet not found");
        }

        Page<LedgerHistoryEntryResponse> history = ledgerService.getWalletHistory(
                wallet.getId(), page, type, status, from, to);

        return ResponseEntity.ok(history);
    }
}