package com.mercia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mercia.enums.LedgerEntryReason;
import com.mercia.enums.LedgerEntryType;
import com.mercia.enums.Status;
import com.mercia.enums.TransactionStatus;
import com.mercia.enums.WalletStatus;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
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
	
	private final TransactionRepository transactionRepo;
	private final UserRepository userRepo;
	private final WalletRepository walletRepo;
	private final LedgerService ledgerService;
	private final FraudDetectionService fraudDetectionService;
    private final DailyWalletSummaryService dailyWalletSummaryService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    
	@Transactional
	public Transaction makeTransactionToOther(String senderEmail, String receiverEmail, BigDecimal amount,
			String requestId,String pin,String remark) {

		// 1. Validate request ID
		if (requestId == null || requestId.isBlank()) {
			throw new IllegalArgumentException("Request ID is required");
		}

		// 2. Check whether this payment request already exists
		Optional<Transaction> existing = transactionRepo.findByRequestId(requestId);

		if (existing.isPresent()) {
			Transaction previous = existing.get();

			// Ensure the same ID isn't reused for a different payment
			boolean samePayment = previous.getSenderWallet().getUser().getEmail().equals(senderEmail)
					&& previous.getReceiverWallet().getUser().getEmail().equals(receiverEmail)
					&& previous.getAmount().compareTo(amount) == 0;

			if (!samePayment) {
				throw new IllegalArgumentException("Request ID already used for a different payment");
			}

			// Return the original transaction; don't debit again
			return previous;
		}

		// 3. Find users
		User sender = userRepo.findByEmail(senderEmail)
				.orElseThrow(() -> new UserNotFoundException("Sender not found"));

		User receiver = userRepo.findByEmail(receiverEmail)
				.orElseThrow(() -> new UserNotFoundException("Receiver not found"));

		// 4. Validate users
		if (sender.getStatus() != Status.ACTIVE || receiver.getStatus() != Status.ACTIVE) {
			throw new IllegalStateException("Both users must be active");
		}
		

		if (sender.getId().equals(receiver.getId())) {
			throw new IllegalArgumentException("Cannot transfer money to yourself");
		}
      
		// pin
		// 4a. Validate transaction PIN
		if (!passwordEncoder.matches(pin, sender.getPIN())) {
		    throw new IllegalArgumentException("Invalid transaction PIN");
		}

		// 5. Validate amount
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero");
		}
	
		// 6. Get wallets
		Wallet senderWallet = sender.getWallet();
		Wallet receiverWallet = receiver.getWallet();

		if (senderWallet == null || receiverWallet == null) {
			throw new IllegalStateException("Wallet not found");
		}

		//check the wallet status for both sender and receiver
		if(senderWallet.getWalletStatus()!=WalletStatus.ACTIVE)
		{
			String senderWalletException = String.format("Your wallet is:%s", senderWallet.getWalletStatus().toString());
			throw new IllegalStateException(senderWalletException);
		}
		
		if(receiverWallet.getWalletStatus()!=WalletStatus.ACTIVE)
		{
			String receiverWalletException = String.format("Receiver wallet is:%s", senderWallet.getWalletStatus().toString());
			throw new IllegalStateException(receiverWalletException);
		}
		fraudDetectionService.checkDuplicateTransfer(
			    senderWallet.getId(),
			    receiverWallet.getId(),
			    senderWallet,
			    amount
			);
		// 7. Check available balance
		if (senderWallet.getBalance().compareTo(amount) < 0) {
			throw new IllegalArgumentException("Insufficient balance");
		}

		// 8. Debit sender only
		senderWallet.setBalance(senderWallet.getBalance().subtract(amount));

		// Update daily outgoing
		dailyWalletSummaryService.checkAndRecordOutgoing(
		    senderWallet,
		    amount
		);
		walletRepo.save(senderWallet);

		// 9. Create pending transaction
		Instant now = Instant.now();

		Transaction transaction = new Transaction();

		transaction.setSenderWallet(senderWallet);
		transaction.setReceiverWallet(receiverWallet);
		transaction.setAmount(amount);
		transaction.setRequestId(requestId);
		transaction.setStatus(TransactionStatus.PENDING);
		transaction.setCreatedAt(now);
		transaction.setSettlementDueAt(now.plusSeconds(60));
		transaction.setRemark(remark);

		// Save first so the transaction has a database ID
		transactionRepo.saveAndFlush(transaction);
		
		// checking if greater than 8k
		fraudDetectionService.checkHighValue(senderWallet, transaction);
		//notify
		notificationService.send(
			    sender,
			    "₹" + amount + " transfer to " + receiver.getName()
			        + " initiated. Expected settlement: " + transaction.getSettlementDueAt()
			);

		// 10. Create sender's debit ledger entry
		ledgerService.recordEntry(senderWallet, transaction, LedgerEntryType.DEBIT, LedgerEntryReason.TRANSFER, amount,
				"Transfer to " + receiverEmail);

		return transaction;
	}

	@Transactional
	public void depositMoney(String email, BigDecimal amount) {
		validatePositiveAmount(amount);
		User user = userRepo.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found. Check the email"));
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

		ledgerService.recordEntry(userWallet, null, LedgerEntryType.CREDIT, LedgerEntryReason.DEPOSIT, amount,
				"Wallet deposit");
		
		dailyWalletSummaryService.recordIncoming(
			    userWallet,
			    amount
			);
		log.info("saved to wallet");
		walletRepo.save(userWallet);

	}

	@Transactional
	public void withdrawMoney(String email, BigDecimal amount) {
		validatePositiveAmount(amount);
		User user = userRepo.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found. Check the email"));
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

		ledgerService.recordEntry(userWallet, null, LedgerEntryType.DEBIT, LedgerEntryReason.WITHDRAWAL, amount,
				"Wallet withdrawal");
		walletRepo.save(userWallet);
		dailyWalletSummaryService.checkAndRecordOutgoing(
			    userWallet,
			    amount
			);

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
