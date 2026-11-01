package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.mercia.dto.LedgerHistoryEntryResponse;
import com.mercia.enums.LedgerEntryReason;
import com.mercia.enums.LedgerEntryType;
import com.mercia.model.LedgerEntry;
import com.mercia.model.Transaction;
import com.mercia.model.Wallet;
import com.mercia.repository.LedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

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
		when(ledgerRepo.findByWalletIdOrderByCreatedAtDesc(9L, PageRequest.of(2, 10)))
				.thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(2, 10), 21));

		Page<LedgerHistoryEntryResponse> result = ledgerService.getWalletHistory(9L, 2);

		assertEquals(2, result.getNumber());
		assertEquals(10, result.getSize());
		assertEquals(21, result.getTotalElements());
		assertEquals(new LedgerHistoryEntryResponse(7L, LedgerEntryType.CREDIT,
				LedgerEntryReason.DEPOSIT, new BigDecimal("18.25"), createdAt, "Wallet deposit"),
				result.getContent().get(0));
		ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
				ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
		verify(ledgerRepo).findByWalletIdOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.eq(9L),
				pageableCaptor.capture());
		assertEquals(PageRequest.of(2, 10), pageableCaptor.getValue());
	}
}
