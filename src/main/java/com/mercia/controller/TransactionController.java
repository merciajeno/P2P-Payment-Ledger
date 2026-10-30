package com.mercia.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.TransactionRequest;
import com.mercia.repository.UserRepository;
import com.mercia.service.TransactionService;

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
    public ResponseEntity<String> depositAmount(@RequestBody TransactionRequest request)
    {
    	String email = request.receiverEmail();
    	BigDecimal amount = request.amount();
    	transactionService.depositMoney(email,amount);
    	return ResponseEntity.ok("Deposited amount");
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAmount(@RequestBody TransactionRequest request)
    {
    	String email = request.receiverEmail();
    	BigDecimal amount = request.amount();
    	transactionService.withdrawMoney(email,amount);
    	return ResponseEntity.ok("Amount withdrawn");
    }
}