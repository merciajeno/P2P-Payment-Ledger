package com.mercia.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.LedgerHistoryEntryResponse;
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
            @RequestParam(defaultValue = "0") int page) {

        User user = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        Wallet wallet = user.getWallet();

        if (wallet == null) {
            throw new IllegalStateException("Wallet not found");
        }

        Page<LedgerHistoryEntryResponse> history =
                ledgerService.getWalletHistory(wallet.getId(), page);

        return ResponseEntity.ok(history);
    }
}