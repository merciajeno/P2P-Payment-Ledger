package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.enums.LedgerEntryReason;
import com.mercia.enums.LedgerEntryType;
import com.mercia.enums.LedgerHistoryType;
import com.mercia.enums.TransactionStatus;
import com.mercia.model.LedgerEntry;
import com.mercia.model.Transaction;
import com.mercia.model.Wallet;
import com.mercia.repository.LedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

	private static final ZoneId HISTORY_ZONE = ZoneId.of("Asia/Kolkata");

	@Mock
	private LedgerEntryRepository ledgerRepo;

	@InjectMocks
	private LedgerService ledgerService;

	@Test
	void recordEntryPersistsAllLedgerFields() {
		Wallet wallet = new Wallet();
		Transaction transaction = new Transaction();
		BigDecimal amount = new BigDecimal("42.50");

		ledgerService.recordEntry(wallet, transaction, LedgerEntryType.DEBIT,
				LedgerEntryReason.TRANSFER, amount, "Transfer to user");

		ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
		verify(ledgerRepo).save(captor.capture());
		LedgerEntry savedEntry = captor.getValue();
		assertSame(wallet, savedEntry.getWallet());
		assertSame(transaction, savedEntry.getTransaction());
		assertEquals(LedgerEntryType.DEBIT, savedEntry.getEntryType());
		assertEquals(LedgerEntryReason.TRANSFER, savedEntry.getReason());
		assertEquals(amount, savedEntry.getAmount());
		assertEquals("Transfer to user", savedEntry.getDescription());
	}

	@Test
	void getWalletHistoryMapsPageToResponseAndUsesTenEntryPageable() {
		Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
		LedgerEntry entry = new LedgerEntry();
		entry.setId(7L);
		entry.setEntryType(LedgerEntryType.CREDIT);
		entry.setReason(LedgerEntryReason.DEPOSIT);
		entry.setAmount(new BigDecimal("18.25"));
		entry.setCreatedAt(createdAt);
		entry.setDescription("Wallet deposit");
		Instant earliest = LocalDate.of(1, 1, 1).atStartOfDay(HISTORY_ZONE).toInstant();
		Instant latest = LocalDate.of(10_000, 1, 1).atStartOfDay(HISTORY_ZONE).toInstant();
		when(ledgerRepo.findWalletHistoryFiltered(9L, "ALL", null, earliest, latest, PageRequest.of(2, 10)))
				.thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(2, 10), 21));

		Page<LedgerHistoryEntryResponse> result = ledgerService.getWalletHistory(9L, 2);

		assertEquals(2, result.getNumber());
		assertEquals(10, result.getSize());
		assertEquals(21, result.getTotalElements());
		assertEquals(new LedgerHistoryEntryResponse(7L, LedgerEntryType.CREDIT,
				LedgerEntryReason.DEPOSIT, new BigDecimal("18.25"), createdAt, "Wallet deposit",
				null, TransactionStatus.SUCCESS, null),
				result.getContent().get(0));
		ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
				ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
		verify(ledgerRepo).findWalletHistoryFiltered(org.mockito.ArgumentMatchers.eq(9L),
				org.mockito.ArgumentMatchers.eq("ALL"), org.mockito.ArgumentMatchers.isNull(),
				org.mockito.ArgumentMatchers.eq(earliest), org.mockito.ArgumentMatchers.eq(latest),
				pageableCaptor.capture());
		assertEquals(PageRequest.of(2, 10), pageableCaptor.getValue());
	}

	@Test
	void getWalletHistoryPassesFiltersAndMapsPendingTransferDetails() {
		Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
		Instant dueAt = createdAt.plusSeconds(60);
		Wallet wallet = new Wallet();
		wallet.setId(9L);
		Wallet receiver = new Wallet();
		receiver.setId(10L);
		Transaction transaction = new Transaction();
		transaction.setSenderWallet(wallet);
		transaction.setReceiverWallet(receiver);
		transaction.setCreatedAt(createdAt);
		transaction.setSettlementDueAt(dueAt);
		transaction.setStatus(TransactionStatus.PENDING);
		LedgerEntry entry = new LedgerEntry();
		entry.setId(8L);
		entry.setWallet(wallet);
		entry.setTransaction(transaction);
		entry.setEntryType(LedgerEntryType.DEBIT);
		entry.setReason(LedgerEntryReason.TRANSFER);
		entry.setAmount(new BigDecimal("12.00"));
		entry.setCreatedAt(createdAt);
		when(ledgerRepo.findWalletHistoryFiltered(
				9L, "SENT", TransactionStatus.PENDING,
				Instant.parse("2026-01-01T18:30:00Z"),
				Instant.parse("2026-01-02T18:30:00Z"), PageRequest.of(0, 10)))
				.thenReturn(new PageImpl<>(List.of(entry)));

		Page<LedgerHistoryEntryResponse> result = ledgerService.getWalletHistory(
				9L, 0, LedgerHistoryType.SENT, TransactionStatus.PENDING,
				LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-02"));

		assertEquals("SENT", result.getContent().get(0).transferType());
		assertEquals(TransactionStatus.PENDING, result.getContent().get(0).status());
		assertEquals(dueAt, result.getContent().get(0).settlementDueAt());
	}

	@ParameterizedTest
	@EnumSource(TransactionStatus.class)
	void getWalletHistoryMapsReceivedTransferStatus(TransactionStatus status) {
		Instant createdAt = Instant.parse("2026-03-04T10:15:30Z");
		Instant dueAt = createdAt.plusSeconds(60);
		Wallet sender = wallet(9L);
		Wallet receiver = wallet(10L);
		Transaction transaction = transaction(sender, receiver, status, createdAt, dueAt);
		LedgerEntry entry = ledgerEntry(transaction, receiver, createdAt);
		when(ledgerRepo.findWalletHistoryFiltered(
				eq(10L), eq("ALL"), eq(null),
				any(Instant.class), any(Instant.class), eq(PageRequest.of(0, 10))))
				.thenReturn(new PageImpl<>(List.of(entry)));

		Page<LedgerHistoryEntryResponse> result = ledgerService.getWalletHistory(10L, 0);

		LedgerHistoryEntryResponse response = result.getContent().get(0);
		assertEquals("RECEIVED", response.transferType());
		assertEquals(status, response.status());
		assertEquals(dueAt, response.settlementDueAt());
	}

	@ParameterizedTest
	@MethodSource("dateFilterCases")
	void getWalletHistoryConvertsOptionalDatesToIstBounds(
			LocalDate from, LocalDate to, Instant expectedFrom, Instant expectedTo) {
		PageRequest pageable = PageRequest.of(1, 10);
		when(ledgerRepo.findWalletHistoryFiltered(
				eq(9L), eq("SENT"), eq(TransactionStatus.SUCCESS),
				any(Instant.class), any(Instant.class), eq(pageable)))
				.thenReturn(Page.empty(pageable));

		ledgerService.getWalletHistory(
				9L, 1, LedgerHistoryType.SENT, TransactionStatus.SUCCESS, from, to);

		ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(ledgerRepo).findWalletHistoryFiltered(
				eq(9L), eq("SENT"), eq(TransactionStatus.SUCCESS),
				fromCaptor.capture(), toCaptor.capture(), eq(pageable));
		assertEquals(expectedFrom, fromCaptor.getValue());
		assertEquals(expectedTo, toCaptor.getValue());
	}

	private static Stream<Arguments> dateFilterCases() {
		LocalDate date = LocalDate.parse("2026-03-04");
		Instant earliest = LocalDate.of(1, 1, 1).atStartOfDay(HISTORY_ZONE).toInstant();
		Instant latest = LocalDate.of(10_000, 1, 1).atStartOfDay(HISTORY_ZONE).toInstant();
		Instant dateStart = date.atStartOfDay(HISTORY_ZONE).toInstant();
		Instant nextDateStart = date.plusDays(1).atStartOfDay(HISTORY_ZONE).toInstant();

		return Stream.of(
				Arguments.of(null, null, earliest, latest),
				Arguments.of(date, null, dateStart, latest),
				Arguments.of(null, date, earliest, nextDateStart),
				Arguments.of(date, date, dateStart, nextDateStart));
	}

	private Wallet wallet(Long id) {
		Wallet wallet = new Wallet();
		wallet.setId(id);
		return wallet;
	}

	private Transaction transaction(
			Wallet sender, Wallet receiver, TransactionStatus status, Instant createdAt, Instant dueAt) {
		Transaction transaction = new Transaction();
		transaction.setSenderWallet(sender);
		transaction.setReceiverWallet(receiver);
		transaction.setStatus(status);
		transaction.setCreatedAt(createdAt);
		transaction.setSettlementDueAt(dueAt);
		return transaction;
	}

	private LedgerEntry ledgerEntry(Transaction transaction, Wallet wallet, Instant createdAt) {
		LedgerEntry entry = new LedgerEntry();
		entry.setId(8L);
		entry.setWallet(wallet);
		entry.setTransaction(transaction);
		entry.setEntryType(LedgerEntryType.CREDIT);
		entry.setReason(LedgerEntryReason.SETTLEMENT);
		entry.setAmount(new BigDecimal("12.00"));
		entry.setCreatedAt(createdAt);
		entry.setDescription("Transfer received");
		return entry;
	}
}
