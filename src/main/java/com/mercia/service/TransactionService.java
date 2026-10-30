package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.mercia.exception.ExcessAmountException;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.Status;
import com.mercia.model.Transaction;
import com.mercia.model.TransactionStatus;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

	private static final BigDecimal MinAmount = new BigDecimal(8000.0);
	private final TransactionRepository transactionRepo;
	private final UserRepository userRepo;
	private final WalletRepository walletRepo;
	
	@Transactional
	public void makeTransactionToOther(
	        String senderEmail,
	        String receiverEmail,
	        BigDecimal amount) {

	    User sender = userRepo.findByEmail(senderEmail)
	            .orElseThrow(() ->
	                new UserNotFoundException("Sender not found"));

	    User receiver = userRepo.findByEmail(receiverEmail)
	            .orElseThrow(() ->
	                new UserNotFoundException("Receiver not found"));

//	    if(amount.compareTo(MinAmount)==1)
//	    {
//	    	throw new ExcessAmountException("Amount is greater than 8000.Not accepted!!");
//	    }
	    // Validate users
	    if (sender.getStatus() != Status.ACTIVE ||
	        receiver.getStatus() != Status.ACTIVE) {
	        throw new IllegalStateException(
	                "Both users must be active");
	    }

	    if (sender.getId().equals(receiver.getId())) {
	        throw new IllegalArgumentException(
	                "Cannot transfer money to yourself");
	    }

	    // Validate amount
	    if (amount == null ||
	        amount.compareTo(BigDecimal.ZERO) <= 0) {
	        throw new IllegalArgumentException(
	                "Amount must be greater than zero");
	    }

	    Wallet senderWallet = sender.getWallet();
	    Wallet receiverWallet = receiver.getWallet();

	    if (senderWallet == null || receiverWallet == null) {
	        throw new IllegalStateException(
	                "Wallet not found");
	    }

	    // Check available balance
	    if (senderWallet.getBalance().compareTo(amount) < 0) {
	        throw new IllegalArgumentException(
	                "Insufficient balance");
	    }

	    // Debit sender only
	    BigDecimal newSenderBalance =
	            senderWallet.getBalance().subtract(amount);

	    senderWallet.setBalance(newSenderBalance);
	    walletRepo.save(senderWallet);

	    // Create pending transaction
	    Transaction transaction = new Transaction();

	    transaction.setSenderWallet(senderWallet);
	    transaction.setReceiverWallet(receiverWallet);
	    transaction.setAmount(amount);
	    transaction.setStatus(TransactionStatus.PENDING);

	    Instant now = Instant.now();

	    transaction.setCreatedAt(now);
	    transaction.setSettlementDueAt(now.plusSeconds(60));

	    transactionRepo.save(transaction);
	}
	
	@Transactional
	public void depositMoney(String email,BigDecimal amount)
	{
		validatePositiveAmount(amount);
		User user = userRepo.findByEmail(email)
				.orElseThrow(()-> new UserNotFoundException("User not found. Check the email"));
		validateActiveUser(user);
		
		Wallet userWallet = user.getWallet();
		if (userWallet == null || userWallet.getBalance() == null) {
			throw new IllegalStateException("Wallet not found");
		}
		if (userWallet.getWalletStatus() != WalletStatus.ACTIVE) {
			throw new IllegalStateException("Wallet is not active");
		}
		BigDecimal addAmount = userWallet.getBalance().add(amount);
		userWallet.setBalance(addAmount);
		
		walletRepo.save(userWallet);
		
	}
	
	@Transactional
	public void withdrawMoney(String email,BigDecimal amount)
	{
		validatePositiveAmount(amount);
		User user = userRepo.findByEmail(email)
				.orElseThrow(()-> new UserNotFoundException("User not found. Check the email"));
		validateActiveUser(user);
		
		Wallet userWallet = user.getWallet();
		if (userWallet == null || userWallet.getBalance() == null) {
			throw new IllegalStateException("Wallet not found");
		}
		if (userWallet.getWalletStatus() != WalletStatus.ACTIVE) {
			throw new IllegalStateException("Wallet is not active");
		}
		if (userWallet.getBalance().compareTo(amount) < 0) {
			throw new IllegalArgumentException("Insufficient balance");
		}
		BigDecimal newBalance = userWallet.getBalance().subtract(amount);
		userWallet.setBalance(newBalance);
		
		walletRepo.save(userWallet);
		
	}

	private void validatePositiveAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero");
		}
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != Status.ACTIVE) {
			throw new IllegalStateException("User account is not active");
		}
	}
}
