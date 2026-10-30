package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.AmountRequest;
import com.mercia.dto.TransactionRequest;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepo;

    @PostMapping()
    public ResponseEntity<String> makeTransactionToOther(
            @RequestBody TransactionRequest request,
            Authentication authentication) {

    	String username = authentication.getName();
        String senderEmail = userRepo.findByName(username).get().getEmail();
        log.debug("Sending from:"+senderEmail);

        transactionService.makeTransactionToOther(
                senderEmail,
                request.receiverEmail(),
                request.amount()
        );

        return ResponseEntity.ok("Transaction initiated successfully");
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<String> depositAmount(
            @Valid @RequestBody AmountRequest request,
            Authentication authentication)
    {
		String email = getAuthenticatedEmail(authentication);
		transactionService.depositMoney(email, request.amount());
		return ResponseEntity.ok("Deposited amount");
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAmount(
            @Valid @RequestBody AmountRequest request,
            Authentication authentication)
    {
		String email = getAuthenticatedEmail(authentication);
		transactionService.withdrawMoney(email, request.amount());
		return ResponseEntity.ok("Amount withdrawn");
    }

    private String getAuthenticatedEmail(Authentication authentication) {
        User user = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
        return user.getEmail();
    }
}