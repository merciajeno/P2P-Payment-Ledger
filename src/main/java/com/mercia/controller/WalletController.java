package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.WalletBalanceResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserRepository userRepository;
    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getWallet() == null || user.getWallet().getBalance() == null) {
            throw new IllegalStateException("Wallet balance unavailable");
        }
        return ResponseEntity.ok(new WalletBalanceResponse(user.getWallet().getBalance()));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<String> deactivate(Authentication authentication) {
        walletService.deactivate(getUser(authentication));
        return ResponseEntity.ok("Wallet deactivated");
    }

    @PostMapping("/reactivate")
    public ResponseEntity<String> reactivate(Authentication authentication) {
        walletService.reactivate(getUser(authentication));
        return ResponseEntity.ok("Wallet reactivated");
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}

