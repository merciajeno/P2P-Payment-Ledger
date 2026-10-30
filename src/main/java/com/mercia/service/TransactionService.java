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
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
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
	
	public void depositMoney(String email,BigDecimal amount)
	{
		User user = userRepo.findByEmail(email)
				.orElseThrow(()-> new UserNotFoundException("User not found. Check the email"));
		
		Wallet userWallet = user.getWallet();
		BigDecimal addAmount = userWallet.getBalance().add(amount);
		userWallet.setBalance(addAmount);
		
		walletRepo.save(userWallet);
		
	}
	
	public void withdrawMoney(String email,BigDecimal amount)
	{
		User user = userRepo.findByEmail(email)
				.orElseThrow(()-> new UserNotFoundException("User not found. Check the email"));
		
		Wallet userWallet = user.getWallet();
		BigDecimal addAmount = userWallet.getBalance().subtract(amount);
		userWallet.setBalance(addAmount);
		
		walletRepo.save(userWallet);
		
	}
}
