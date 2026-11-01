package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.mercia.dto.AdminDashboardResponse;
import com.mercia.dto.AdminTransactionResponse;
import com.mercia.dto.AdminUserResponse;
import com.mercia.enums.BatchStatus;
import com.mercia.enums.FraudAlertStatus;
import com.mercia.enums.FraudAlertType;
import com.mercia.enums.TransactionStatus;
import com.mercia.enums.WalletStatus;
import com.mercia.model.FraudAlert;
import com.mercia.model.SettlementBatch;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.SettlementBatchRepository;
import com.mercia.repository.TransactionRepository;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

	private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

	@Mock
	private UserRepository userRepo;
	@Mock
	private WalletRepository walletRepo;
	@Mock
	private TransactionRepository transactionRepo;
	@Mock
	private FraudAlertRepository fraudAlertRepo;
	@Mock
	private SettlementBatchRepository settlementBatchRepo;
	@Mock
	private SettlementService settlementService;
	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private AdminService adminService;

	@Test
	void getDashboardAggregatesDailyMetricsAndPendingAmounts() {
		when(userRepo.count()).thenReturn(7L);
		when(transactionRepo.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(5L);
		when(transactionRepo.countByStatusAndSettledAtBetween(
				eq(TransactionStatus.SUCCESS), any(Instant.class), any(Instant.class))).thenReturn(3L);
		when(transactionRepo.sumAmountByCreatedAtBetween(any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("125.50"));
		when(transactionRepo.sumPendingAmount()).thenReturn(new BigDecimal("45.00"));
		when(fraudAlertRepo.countByStatus(FraudAlertStatus.PENDING)).thenReturn(2L);

		AdminDashboardResponse response = adminService.getDashboard();

		assertEquals(7L, response.totalUsers());
		assertEquals(5L, response.todayTransfersInitiated());
		assertEquals(3L, response.todayTransfersSettled());
		assertEquals(new BigDecimal("125.50"), response.todayTransactionVolume());
		assertEquals(new BigDecimal("45.00"), response.totalPendingSettlementAmount());
		assertEquals(2L, response.pendingFraudAlertsCount());
		LocalDate today = LocalDate.now(INDIA_ZONE);
		Instant start = today.atStartOfDay(INDIA_ZONE).toInstant();
		Instant end = today.plusDays(1).atStartOfDay(INDIA_ZONE).toInstant();
		verify(transactionRepo).countByCreatedAtBetween(start, end);
		verify(transactionRepo).countByStatusAndSettledAtBetween(TransactionStatus.SUCCESS, start, end);
	}

	@Test
	void getAllUsersMapsWalletAndDefaultsMissingWalletValues() {
		User walletOwner = user(11L, "Alice");
		Wallet wallet = wallet("15.25", WalletStatus.ACTIVE);
		walletOwner.setWallet(wallet);
		User withoutWallet = user(12L, "Bob");
		when(userRepo.findAll()).thenReturn(List.of(walletOwner, withoutWallet));

		List<AdminUserResponse> responses = adminService.getAllUsers();

		assertEquals(2, responses.size());
		assertEquals("Alice", responses.get(0).name());
		assertEquals(new BigDecimal("15.25"), responses.get(0).walletBalance());
		assertEquals(WalletStatus.ACTIVE, responses.get(0).walletStatus());
		assertEquals(BigDecimal.ZERO, responses.get(1).walletBalance());
		assertEquals(null, responses.get(1).walletStatus());
	}

	@Test
	void freezeAndUnfreezeWalletUpdateAndSaveWallet() {
		User user = user(13L, "Carol");
		Wallet wallet = wallet("20.00", WalletStatus.ACTIVE);
		user.setWallet(wallet);
		when(userRepo.findById(13L)).thenReturn(Optional.of(user));

		adminService.freezeWallet(13L);
		assertEquals(WalletStatus.FROZEN, wallet.getWalletStatus());
		verify(walletRepo).save(wallet);

		adminService.unfreezeWallet(13L);
		assertEquals(WalletStatus.ACTIVE, wallet.getWalletStatus());
		verify(walletRepo, org.mockito.Mockito.times(2)).save(wallet);
	}

	@Test
	void freezeWalletRejectsUnknownUserOrMissingWallet() {
		when(userRepo.findById(99L)).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> adminService.freezeWallet(99L));

		User user = user(100L, "No Wallet");
		when(userRepo.findById(100L)).thenReturn(Optional.of(user));
		assertThrows(IllegalStateException.class, () -> adminService.freezeWallet(100L));
		verify(walletRepo, never()).save(any(Wallet.class));
	}

	@Test
	void getAllTransactionsUsesTwentyItemPagesAndMapsTransactionFields() {
		Transaction transaction = transaction(21L, TransactionStatus.PENDING);
		Page<Transaction> transactions = new PageImpl<>(List.of(transaction));
		when(transactionRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(2, 20))).thenReturn(transactions);

		Page<AdminTransactionResponse> responses = adminService.getAllTransactions(2);

		assertEquals(1, responses.getTotalElements());
		assertEquals(21L, responses.getContent().get(0).id());
		assertEquals("Sender", responses.getContent().get(0).senderName());
		assertEquals("Receiver", responses.getContent().get(0).receiverName());
		assertEquals(transaction.getAmount(), responses.getContent().get(0).amount());
		assertEquals(TransactionStatus.PENDING, responses.getContent().get(0).status());
		verify(transactionRepo).findAllByOrderByCreatedAtDesc(PageRequest.of(2, 20));
	}

	@Test
	void getPendingTransactionsReturnsMappedPendingRecords() {
		Transaction transaction = transaction(22L, TransactionStatus.PENDING);
		when(transactionRepo.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(transaction));

		List<AdminTransactionResponse> responses = adminService.getPendingTransactions();

		assertEquals(1, responses.size());
		assertEquals(22L, responses.get(0).id());
		assertEquals(TransactionStatus.PENDING, responses.get(0).status());
		verify(transactionRepo).findByStatus(TransactionStatus.PENDING);
	}

	@Test
	void manualSettleDelegatesPendingTransactionToSettlementService() {
		Transaction transaction = transaction(31L, TransactionStatus.PENDING);
		when(transactionRepo.findById(31L)).thenReturn(Optional.of(transaction));

		adminService.manualSettle(31L);

		verify(settlementService).settleOneTransaction(transaction);
	}

	@Test
	void manualSettleRejectsMissingOrNonPendingTransaction() {
		when(transactionRepo.findById(32L)).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> adminService.manualSettle(32L));

		Transaction completed = transaction(33L, TransactionStatus.SUCCESS);
		when(transactionRepo.findById(33L)).thenReturn(Optional.of(completed));
		assertThrows(IllegalStateException.class, () -> adminService.manualSettle(33L));
		verify(settlementService, never()).settleOneTransaction(any(Transaction.class));
	}

	@Test
	void getPendingFraudAlertsMapsAlertAndWalletOwner() {
		User owner = user(41L, "Dana");
		Wallet wallet = wallet("80.00", WalletStatus.ACTIVE);
		wallet.setUser(owner);
		Transaction transaction = transaction(42L, TransactionStatus.PENDING);
		FraudAlert alert = fraudAlert(43L, wallet, transaction);
		when(fraudAlertRepo.findByStatusOrderByCreatedAtDesc(FraudAlertStatus.PENDING))
				.thenReturn(List.of(alert));

		var responses = adminService.getPendingFraudAlerts();

		assertEquals(1, responses.size());
		assertEquals(43L, responses.get(0).alertId());
		assertEquals(FraudAlertType.HIGH_VALUE_TRANSACTION, responses.get(0).alertType());
		assertEquals(42L, responses.get(0).transactionId());
		assertEquals("Dana", responses.get(0).walletOwnerName());
	}

	@Test
	void blockTransactionRefundsSenderAndResolvesAlert() {
		User sender = user(51L, "Eli");
		Wallet senderWallet = wallet("30.00", WalletStatus.ACTIVE);
		senderWallet.setUser(sender);
		Transaction transaction = transaction(52L, TransactionStatus.PENDING);
		transaction.setSenderWallet(senderWallet);
		transaction.setAmount(new BigDecimal("12.50"));
		FraudAlert alert = fraudAlert(53L, senderWallet, transaction);
		when(fraudAlertRepo.findById(53L)).thenReturn(Optional.of(alert));

		adminService.blockTransaction(53L);

		assertEquals(new BigDecimal("42.50"), senderWallet.getBalance());
		assertEquals(TransactionStatus.FAILED, transaction.getStatus());
		assertEquals(FraudAlertStatus.BLOCKED, alert.getStatus());
		assertNotNull(alert.getResolvedAt());
		verify(walletRepo).save(senderWallet);
		verify(transactionRepo).save(transaction);
		verify(fraudAlertRepo).save(alert);
		verify(notificationService).send(sender,
				"₹12.50 transfer was blocked by admin. Amount refunded to your wallet");
	}

	@Test
	void blockTransactionRejectsUnlinkedOrNoLongerPendingTransaction() {
		Wallet wallet = wallet("5.00", WalletStatus.ACTIVE);
		FraudAlert unlinkedAlert = fraudAlert(61L, wallet, null);
		when(fraudAlertRepo.findById(61L)).thenReturn(Optional.of(unlinkedAlert));
		assertThrows(IllegalStateException.class, () -> adminService.blockTransaction(61L));

		Transaction completed = transaction(62L, TransactionStatus.SUCCESS);
		FraudAlert resolvedAlert = fraudAlert(63L, wallet, completed);
		when(fraudAlertRepo.findById(63L)).thenReturn(Optional.of(resolvedAlert));
		assertThrows(IllegalStateException.class, () -> adminService.blockTransaction(63L));
		verify(walletRepo, never()).save(any(Wallet.class));
		verify(transactionRepo, never()).save(any(Transaction.class));
	}

	@Test
	void dismissAlertMarksAlertResolved() {
		FraudAlert alert = fraudAlert(71L, wallet("4.00", WalletStatus.ACTIVE), null);
		when(fraudAlertRepo.findById(71L)).thenReturn(Optional.of(alert));

		adminService.dismissAlert(71L);

		assertEquals(FraudAlertStatus.DISMISSED, alert.getStatus());
		assertNotNull(alert.getResolvedAt());
		verify(fraudAlertRepo).save(alert);
	}

	@Test
	void getRecentJobsMapsSettlementBatchFields() {
		SettlementBatch batch = new SettlementBatch();
		batch.setId(81L);
		batch.setStartedAt(Instant.parse("2025-01-01T10:00:00Z"));
		batch.setCompletedAt(Instant.parse("2025-01-01T10:01:00Z"));
		batch.setStatus(BatchStatus.PARTIAL_SUCCESS);
		batch.setTotalProcessed(10);
		batch.setSettledCount(7);
		batch.setFailedCount(2);
		batch.setDeclinedCount(1);
		batch.setTotalAmountSettled(new BigDecimal("70.00"));
		when(settlementBatchRepo.findTop20ByOrderByStartedAtDesc()).thenReturn(List.of(batch));

		var responses = adminService.getRecentJobs();

		assertEquals(1, responses.size());
		assertEquals(81L, responses.get(0).id());
		assertEquals(BatchStatus.PARTIAL_SUCCESS, responses.get(0).status());
		assertEquals(10, responses.get(0).totalProcessed());
		assertEquals(new BigDecimal("70.00"), responses.get(0).totalAmountSettled());
	}

	private User user(Long id, String name) {
		User user = new User();
		user.setId(id);
		user.setName(name);
		user.setEmail(name.toLowerCase() + "@example.com");
		return user;
	}

	private Wallet wallet(String balance, WalletStatus status) {
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal(balance));
		wallet.setWalletStatus(status);
		return wallet;
	}

	private Transaction transaction(Long id, TransactionStatus status) {
		Transaction transaction = new Transaction();
		transaction.setId(id);
		transaction.setAmount(new BigDecimal("10.00"));
		transaction.setStatus(status);
		transaction.setRemark("test transfer");
		transaction.setCreatedAt(Instant.parse("2025-02-01T10:00:00Z"));
		transaction.setSettlementDueAt(Instant.parse("2025-02-01T10:05:00Z"));
		transaction.setSenderWallet(walletWithOwner("Sender"));
		transaction.setReceiverWallet(walletWithOwner("Receiver"));
		return transaction;
	}

	private Wallet walletWithOwner(String name) {
		Wallet wallet = wallet("10.00", WalletStatus.ACTIVE);
		wallet.setUser(user(1L, name));
		return wallet;
	}

	private FraudAlert fraudAlert(Long id, Wallet wallet, Transaction transaction) {
		FraudAlert alert = new FraudAlert();
		alert.setId(id);
		alert.setWallet(wallet);
		alert.setTransaction(transaction);
		alert.setAlertType(FraudAlertType.HIGH_VALUE_TRANSACTION);
		alert.setStatus(FraudAlertStatus.PENDING);
		alert.setAmount(new BigDecimal("10.00"));
		alert.setDescription("Review transfer");
		alert.setCreatedAt(Instant.parse("2025-02-01T10:00:00Z"));
		return alert;
	}
}
