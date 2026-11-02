package com.mercia.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.AdminDashboardResponse;
import com.mercia.dto.AdminTransactionResponse;
import com.mercia.dto.AdminUserResponse;
import com.mercia.dto.FraudAlertResponse;
import com.mercia.dto.SettlementBatchResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    // helper
    private User getAdmin(Authentication authentication) {
        return userRepository.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("Admin not found"));
    }

    // Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    // User list
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{userId}/freeze")
    public ResponseEntity<String> freezeWallet(@PathVariable Long userId,Authentication authentication) {
        adminService.freezeWallet(userId,getAdmin(authentication));
        return ResponseEntity.ok("Wallet frozen");
    }

    @PostMapping("/users/{userId}/unfreeze")
    public ResponseEntity<String> unfreezeWallet(@PathVariable Long userId,Authentication authentication) {
        adminService.unfreezeWallet(userId,getAdmin(authentication));
        return ResponseEntity.ok("Wallet unfrozen");
    }

    // Transaction monitoring
    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminService.getAllTransactions(page));
    }

    // Settlement monitor
    @GetMapping("/settlements/pending")
    public ResponseEntity<List<AdminTransactionResponse>> getPendingSettlements() {
        return ResponseEntity.ok(adminService.getPendingTransactions());
    }

    @PostMapping("/settlements/{transactionId}/settle")
    public ResponseEntity<String> manualSettle(@PathVariable Long transactionId,Authentication authentication) {
        adminService.manualSettle(transactionId,getAdmin(authentication));
        return ResponseEntity.ok("Transaction settled");
    }

    // Fraud review
    @GetMapping("/fraud-alerts")
    public ResponseEntity<List<FraudAlertResponse>> getFraudAlerts() {
        return ResponseEntity.ok(adminService.getPendingFraudAlerts());
    }

    @PostMapping("/fraud-alerts/{alertId}/block")
    public ResponseEntity<String> blockTransaction(@PathVariable Long alertId,Authentication authentication) {
        adminService.blockTransaction(alertId,getAdmin(authentication));
        return ResponseEntity.ok("Transaction blocked and sender refunded");
    }

    @PostMapping("/fraud-alerts/{alertId}/dismiss")
    public ResponseEntity<String> dismissAlert(@PathVariable Long alertId,Authentication authentication) {
        adminService.dismissAlert(alertId,getAdmin(authentication));
        return ResponseEntity.ok("Alert dismissed");
    }

    // Job monitor
    @GetMapping("/jobs")
    public ResponseEntity<List<SettlementBatchResponse>> getRecentJobs() {
        return ResponseEntity.ok(adminService.getRecentJobs());
    }
}
