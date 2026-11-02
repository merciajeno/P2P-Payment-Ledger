package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	private static final BigDecimal AMOUNT = new BigDecimal("25.00");

	@Mock
	private TransactionRepository transactionRepo;
	@Mock
	private UserRepository userRepo;
	@Mock
	private WalletRepository walletRepo;
	@Mock
	private LedgerService ledgerService;
	@Mock
	private FraudDetectionService fraudDetectionService;
	@Mock
	private DailyWalletSummaryService dailyWalletSummaryService;
	@Mock
	private NotificationService notificationService;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private TransactionService transactionService;

	private User sender;
	private User receiver;
	private Wallet senderWallet;
	private Wallet receiverWallet;

	@BeforeEach
	void setUp() {
		sender = createUser(1L, "sender", "sender@example.com");
		receiver = createUser(2L, "receiver", "receiver@example.com");
		senderWallet = createWallet(sender, 1L, "100.00");
		receiverWallet = createWallet(receiver, 2L, "10.00");
		sender.setWallet(senderWallet);
		receiver.setWallet(receiverWallet);
	}

	@Test
	void makeTransferRejectsMissingOrBlankRequestId() {
		assertThrows(IllegalArgumentException.class,
				() -> transfer(null, sender.getEmail(), receiver.getEmail(), AMOUNT));
		assertThrows(IllegalArgumentException.class,
				() -> transfer(" ", sender.getEmail(), receiver.getEmail(), AMOUNT));
		verifyNoInteractions(transactionRepo, userRepo);
	}

	@Test
	void makeTransferReturnsPreviousTransactionForIdempotentRequest() {
		Transaction previous = new Transaction();
		previous.setSenderWallet(senderWallet);
		previous.setReceiverWallet(receiverWallet);
		previous.setAmount(AMOUNT);
		when(transactionRepo.findByRequestId("request-1")).thenReturn(Optional.of(previous));

		Transaction result = transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT);

		assertSame(previous, result);
		verifyNoInteractions(userRepo, passwordEncoder, walletRepo, ledgerService,
				fraudDetectionService, dailyWalletSummaryService, notificationService);
	}

	@Test
	void makeTransferRejectsRequestIdReusedForDifferentPayment() {
		Transaction previous = new Transaction();
		previous.setSenderWallet(senderWallet);
		previous.setReceiverWallet(receiverWallet);
		previous.setAmount(AMOUNT);
		when(transactionRepo.findByRequestId("request-1")).thenReturn(Optional.of(previous));

		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-1", "other@example.com", receiver.getEmail(), AMOUNT));
	}

	@Test
	void makeTransferThrowsWhenSenderDoesNotExist() {
		when(transactionRepo.findByRequestId("request-1")).thenReturn(Optional.empty());
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));
	}

	@Test
	void makeTransferThrowsWhenReceiverDoesNotExist() {
		when(transactionRepo.findByRequestId("request-1")).thenReturn(Optional.empty());
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
		when(userRepo.findByEmail(receiver.getEmail())).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));
	}

	@Test
	void makeTransferRejectsInactiveUsersAndSelfTransfers() {
		prepareUsers();
		sender.setStatus(Status.INACTIVE);
		assertThrows(IllegalStateException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));

		sender.setStatus(Status.ACTIVE);
		when(userRepo.findByEmail(receiver.getEmail())).thenReturn(Optional.of(sender));
		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));
	}

	@Test
	void makeTransferRejectsIncorrectPin() {
		prepareUsers();
		when(passwordEncoder.matches("0000", sender.getPIN())).thenReturn(false);

		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT, "0000"));

		verify(fraudDetectionService, never()).checkDuplicateTransfer(any(), any(), any(), any());
	}

	@Test
	void makeTransferRejectsNullOrNonPositiveAmount() {
		prepareUsers();
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), null));
		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-2", sender.getEmail(), receiver.getEmail(), BigDecimal.ZERO));
	}

	@Test
	void makeTransferRejectsMissingWallet() {
		prepareUsers();
		sender.setWallet(null);
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);

		assertThrows(IllegalStateException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));
	}

	@Test
	void makeTransferRejectsInactiveSenderWallet() {
		prepareUsers();
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);
		senderWallet.setWalletStatus(WalletStatus.FROZEN);

		assertThrows(IllegalStateException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));

		verifyNoInteractions(fraudDetectionService, dailyWalletSummaryService, ledgerService,
				notificationService);
		verify(walletRepo, never()).save(any(Wallet.class));
	}

	@Test
	void makeTransferRejectsInactiveReceiverWallet() {
		prepareUsers();
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);
		receiverWallet.setWalletStatus(WalletStatus.FROZEN);

		assertThrows(IllegalStateException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));

		verifyNoInteractions(fraudDetectionService, dailyWalletSummaryService, ledgerService,
				notificationService);
		verify(walletRepo, never()).save(any(Wallet.class));
	}

	@Test
	void makeTransferRejectsInsufficientBalance() {
		prepareUsers();
		senderWallet.setBalance(new BigDecimal("10.00"));
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT));

		verify(fraudDetectionService).checkDuplicateTransfer(1L, 2L, senderWallet, AMOUNT);
		verify(walletRepo, never()).save(any(Wallet.class));
	}

	@Test
	void makeTransferDebitsSenderAndCreatesPendingTransaction() {
		prepareUsers();
		when(passwordEncoder.matches("1234", sender.getPIN())).thenReturn(true);

		Transaction result = transfer("request-1", sender.getEmail(), receiver.getEmail(), AMOUNT);

		assertEquals(new BigDecimal("75.00"), senderWallet.getBalance());
		assertSame(senderWallet, result.getSenderWallet());
		assertSame(receiverWallet, result.getReceiverWallet());
		assertEquals(AMOUNT, result.getAmount());
		assertEquals("request-1", result.getRequestId());
		assertEquals(TransactionStatus.PENDING, result.getStatus());
		assertEquals("test transfer", result.getRemark());
		assertEquals(60, result.getSettlementDueAt().getEpochSecond() - result.getCreatedAt().getEpochSecond());
		verify(dailyWalletSummaryService).checkAndRecordOutgoing(senderWallet, AMOUNT);
		verify(walletRepo).save(senderWallet);
		
	}

	@Test
	void depositMoneyAddsBalanceAndRecordsCredit() {
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));

		transactionService.depositMoney(sender.getEmail(), AMOUNT);

		assertEquals(new BigDecimal("125.00"), senderWallet.getBalance());
		verify(ledgerService).recordEntry(senderWallet, null, LedgerEntryType.CREDIT,
				LedgerEntryReason.DEPOSIT, AMOUNT, "Wallet deposit");
		verify(dailyWalletSummaryService).recordIncoming(senderWallet, AMOUNT);
		verify(walletRepo).save(senderWallet);
	}

	@Test
	void depositMoneyRejectsInvalidAmountMissingUserInactiveUserAndWalletProblems() {
		assertThrows(IllegalArgumentException.class,
				() -> transactionService.depositMoney(sender.getEmail(), BigDecimal.ZERO));
		assertThrows(IllegalArgumentException.class,
				() -> transactionService.depositMoney(sender.getEmail(), null));

		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class,
				() -> transactionService.depositMoney(sender.getEmail(), AMOUNT));

		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
		sender.setStatus(Status.INACTIVE);
		assertThrows(IllegalStateException.class,
				() -> transactionService.depositMoney(sender.getEmail(), AMOUNT));
		sender.setStatus(Status.ACTIVE);

		sender.setWallet(null);
		assertThrows(IllegalStateException.class,
				() -> transactionService.depositMoney(sender.getEmail(), AMOUNT));
		sender.setWallet(senderWallet);
		senderWallet.setBalance(null);
		assertThrows(IllegalStateException.class,
				() -> transactionService.depositMoney(sender.getEmail(), AMOUNT));
		senderWallet.setBalance(BigDecimal.ZERO);
		senderWallet.setWalletStatus(WalletStatus.INACTIVE);
		assertThrows(IllegalStateException.class,
				() -> transactionService.depositMoney(sender.getEmail(), AMOUNT));
	}

	@Test
	void withdrawMoneyDeductsBalanceAndRecordsDebit() {
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));

		transactionService.withdrawMoney(sender.getEmail(), AMOUNT);

		assertEquals(new BigDecimal("75.00"), senderWallet.getBalance());
		verify(ledgerService).recordEntry(senderWallet, null, LedgerEntryType.DEBIT,
				LedgerEntryReason.WITHDRAWAL, AMOUNT, "Wallet withdrawal");
		verify(walletRepo).save(senderWallet);
		verify(dailyWalletSummaryService).checkAndRecordOutgoing(senderWallet, AMOUNT);
	}

	@Test
	void withdrawMoneyRejectsInvalidAmountMissingUserAndInactiveUser() {
		assertThrows(IllegalArgumentException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), BigDecimal.ZERO));
		assertThrows(IllegalArgumentException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), null));

		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));

		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
		sender.setStatus(Status.INACTIVE);
		assertThrows(IllegalStateException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));
	}

	@Test
	void withdrawMoneyRejectsMissingInactiveAndInsufficientWallet() {
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
		sender.setWallet(null);
		assertThrows(IllegalStateException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));

		sender.setWallet(senderWallet);
		senderWallet.setBalance(null);
		assertThrows(IllegalStateException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));

		senderWallet.setBalance(new BigDecimal("100.00"));
		senderWallet.setWalletStatus(WalletStatus.INACTIVE);
		assertThrows(IllegalStateException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));

		senderWallet.setWalletStatus(WalletStatus.ACTIVE);
		senderWallet.setBalance(new BigDecimal("10.00"));
		assertThrows(IllegalArgumentException.class,
				() -> transactionService.withdrawMoney(sender.getEmail(), AMOUNT));
	}

	private void prepareUsers() {
		when(transactionRepo.findByRequestId("request-1")).thenReturn(Optional.empty());
		when(userRepo.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
		when(userRepo.findByEmail(receiver.getEmail())).thenReturn(Optional.of(receiver));
	}

	private Transaction transfer(String requestId, String senderEmail, String receiverEmail, BigDecimal amount) {
		return transfer(requestId, senderEmail, receiverEmail, amount, "1234");
	}

	private Transaction transfer(String requestId, String senderEmail, String receiverEmail, BigDecimal amount,
			String pin) {
		return transactionService.makeTransactionToOther(senderEmail, receiverEmail, amount, requestId, pin,
				"test transfer");
	}

	private User createUser(Long id, String name, String email) {
		User user = new User();
		user.setId(id);
		user.setName(name);
		user.setEmail(email);
		user.setPIN("encoded-pin");
		user.setStatus(Status.ACTIVE);
		return user;
	}

	private Wallet createWallet(User user, Long id, String balance) {
		Wallet wallet = new Wallet();
		wallet.setId(id);
		wallet.setUser(user);
		wallet.setBalance(new BigDecimal(balance));
		wallet.setWalletStatus(WalletStatus.ACTIVE);
		return wallet;
	}
}
