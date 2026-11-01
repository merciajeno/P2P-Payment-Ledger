package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.enums.BatchStatus;
import com.mercia.enums.TransactionStatus;
import com.mercia.enums.WalletStatus;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.SettlementBatchRepository;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

	@Mock
	private TransactionRepository transactionRepo;
	@Mock
	private WalletRepository walletRepo;
	@Mock
	private LedgerService ledgerService;
	@Mock
	private DailyWalletSummaryService dailyWalletSummaryService;
	@Mock
	private SettlementBatchRepository settlementBatchRepo;
	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private SettlementService settlementService;

	@Test
	void settleDueTransactionsClosesEmptyBatchSuccessfully() {
		when(transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
				org.mockito.ArgumentMatchers.eq(TransactionStatus.PENDING), any(Instant.class)))
				.thenReturn(List.of());
		settlementService.settleDueTransactions();

		SettlementBatchSnapshot batch = captureSavedBatch();
		assertEquals(BatchStatus.SUCCESS, batch.status());
		assertEquals(0, batch.totalProcessed());
		assertEquals(0, batch.settledCount());
		assertEquals(BigDecimal.ZERO, batch.totalAmountSettled());
	}

	@Test
	void settleDueTransactionsCreditsReceiverAndRecordsSettlement() {
		Transaction transaction = transaction("50.00", WalletStatus.ACTIVE);
		when(transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
				org.mockito.ArgumentMatchers.eq(TransactionStatus.PENDING), any(Instant.class)))
				.thenReturn(List.of(transaction));

		settlementService.settleDueTransactions();

		assertEquals(new BigDecimal("60.00"), transaction.getReceiverWallet().getBalance());
		assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
		assertEquals(1, captureSavedBatch().settledCount());
		assertEquals(new BigDecimal("50.00"), captureSavedBatch().totalAmountSettled());
		verify(transactionRepo).save(transaction);
		verify(walletRepo).save(transaction.getReceiverWallet());
		verify(dailyWalletSummaryService).recordIncoming(transaction.getReceiverWallet(), transaction.getAmount());
		verify(ledgerService).recordEntry(transaction.getReceiverWallet(), transaction,
				com.mercia.enums.LedgerEntryType.CREDIT, com.mercia.enums.LedgerEntryReason.SETTLEMENT,
				transaction.getAmount(), "Transfer received");
		verify(notificationService).send(transaction.getSenderWallet().getUser(),
				"₹50.00 transfer to receiver has been settled");
		verify(notificationService).send(transaction.getReceiverWallet().getUser(),
				"₹50.00 received from sender");
	}

	@Test
	void settleDueTransactionsDeclinesAndRefundsWhenReceiverWalletIsInactive() {
		Transaction transaction = transaction("30.00", WalletStatus.FROZEN);
		when(transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
				org.mockito.ArgumentMatchers.eq(TransactionStatus.PENDING), any(Instant.class)))
				.thenReturn(List.of(transaction));

		settlementService.settleDueTransactions();

		assertEquals(new BigDecimal("130.00"), transaction.getSenderWallet().getBalance());
		assertEquals(TransactionStatus.DECLINED, transaction.getStatus());
		SettlementBatchSnapshot batch = captureSavedBatch();
		assertEquals(BatchStatus.SUCCESS, batch.status());
		assertEquals(1, batch.declinedCount());
		verify(ledgerService).recordEntry(transaction.getSenderWallet(), transaction,
				com.mercia.enums.LedgerEntryType.CREDIT, com.mercia.enums.LedgerEntryReason.REFUND,
				transaction.getAmount(), "Transfer refund - receiver wallet is inactive");
	}

	@Test
	void settleDueTransactionsMarksBatchFailedAndRefundsWhenProcessingThrows() {
		Transaction transaction = transaction("20.00", WalletStatus.ACTIVE);
		when(transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
				org.mockito.ArgumentMatchers.eq(TransactionStatus.PENDING), any(Instant.class)))
				.thenReturn(List.of(transaction));
		doThrow(new IllegalStateException("summary unavailable"))
				.when(dailyWalletSummaryService)
				.recordIncoming(transaction.getReceiverWallet(), transaction.getAmount());

		settlementService.settleDueTransactions();

		assertEquals(new BigDecimal("120.00"), transaction.getSenderWallet().getBalance());
		assertEquals(TransactionStatus.FAILED, transaction.getStatus());
		assertEquals(BatchStatus.FAILED, captureSavedBatch().status());
		verify(transactionRepo).save(transaction);
		verify(walletRepo).save(transaction.getSenderWallet());
		verify(notificationService).send(transaction.getSenderWallet().getUser(),
				"₹20.00 transfer failed. Amount refunded to your wallet");
	}

	@Test
	void settleDueTransactionsMarksMixedOutcomesAsPartialSuccess() {
		Transaction successful = transaction("10.00", WalletStatus.ACTIVE);
		Transaction failed = transaction("20.00", WalletStatus.ACTIVE);
		when(transactionRepo.findByStatusAndSettlementDueAtLessThanEqual(
				org.mockito.ArgumentMatchers.eq(TransactionStatus.PENDING), any(Instant.class)))
				.thenReturn(List.of(successful, failed));
		doAnswer(invocation -> {
			Wallet wallet = invocation.getArgument(0);
			if (wallet == failed.getReceiverWallet()) {
				throw new IllegalStateException("summary unavailable");
			}
			return null;
		}).when(dailyWalletSummaryService).recordIncoming(any(Wallet.class), any(BigDecimal.class));

		settlementService.settleDueTransactions();

		SettlementBatchSnapshot batch = captureSavedBatch();
		assertEquals(BatchStatus.PARTIAL_SUCCESS, batch.status());
		assertEquals(2, batch.totalProcessed());
		assertEquals(1, batch.settledCount());
		assertEquals(1, batch.failedCount());
		assertEquals(TransactionStatus.SUCCESS, successful.getStatus());
		assertEquals(TransactionStatus.FAILED, failed.getStatus());
	}

	private SettlementBatchSnapshot captureSavedBatch() {
		ArgumentCaptor<com.mercia.model.SettlementBatch> captor =
				ArgumentCaptor.forClass(com.mercia.model.SettlementBatch.class);
		verify(settlementBatchRepo, org.mockito.Mockito.times(2)).save(captor.capture());
		com.mercia.model.SettlementBatch batch = captor.getAllValues().get(1);
		return new SettlementBatchSnapshot(batch.getStatus(), batch.getTotalProcessed(),
				batch.getSettledCount(), batch.getFailedCount(), batch.getDeclinedCount(),
				batch.getTotalAmountSettled());
	}

	private Transaction transaction(String amount, WalletStatus receiverStatus) {
		User sender = user("sender");
		User receiver = user("receiver");
		Wallet senderWallet = wallet(sender, "100.00", WalletStatus.ACTIVE);
		Wallet receiverWallet = wallet(receiver, "10.00", receiverStatus);
		Transaction transaction = new Transaction();
		transaction.setId(41L);
		transaction.setSenderWallet(senderWallet);
		transaction.setReceiverWallet(receiverWallet);
		transaction.setAmount(new BigDecimal(amount));
		transaction.setStatus(TransactionStatus.PENDING);
		return transaction;
	}

	private User user(String name) {
		User user = new User();
		user.setName(name);
		user.setEmail(name + "@example.com");
		return user;
	}

	private Wallet wallet(User user, String balance, WalletStatus status) {
		Wallet wallet = new Wallet();
		wallet.setUser(user);
		wallet.setBalance(new BigDecimal(balance));
		wallet.setWalletStatus(status);
		return wallet;
	}

	private record SettlementBatchSnapshot(BatchStatus status, Integer totalProcessed,
			Integer settledCount, Integer failedCount, Integer declinedCount, BigDecimal totalAmountSettled) {
	}
}
