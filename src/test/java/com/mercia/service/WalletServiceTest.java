package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	@Mock
	private WalletRepository walletRepo;

	@InjectMocks
	private WalletService walletService;

	@Test
	void deactivateChangesActiveWalletToInactive() {
		Wallet wallet = wallet(WalletStatus.ACTIVE);

		walletService.deactivate(userWith(wallet));

		assertEquals(WalletStatus.INACTIVE, wallet.getWalletStatus());
		verify(walletRepo).save(wallet);
	}

	@Test
	void deactivateRejectsFrozenInactiveAndMissingWallet() {
		Wallet frozenWallet = wallet(WalletStatus.FROZEN);
		assertThrows(IllegalStateException.class, () -> walletService.deactivate(userWith(frozenWallet)));

		Wallet inactiveWallet = wallet(WalletStatus.INACTIVE);
		assertThrows(IllegalStateException.class, () -> walletService.deactivate(userWith(inactiveWallet)));
		assertThrows(IllegalStateException.class, () -> walletService.deactivate(new User()));

		verify(walletRepo, never()).save(frozenWallet);
		verify(walletRepo, never()).save(inactiveWallet);
	}

	@Test
	void reactivateChangesInactiveWalletToActive() {
		Wallet wallet = wallet(WalletStatus.INACTIVE);

		walletService.reactivate(userWith(wallet));

		assertEquals(WalletStatus.ACTIVE, wallet.getWalletStatus());
		verify(walletRepo).save(wallet);
	}

	@Test
	void reactivateRejectsFrozenActiveAndMissingWallet() {
		Wallet frozenWallet = wallet(WalletStatus.FROZEN);
		assertThrows(IllegalStateException.class, () -> walletService.reactivate(userWith(frozenWallet)));

		Wallet activeWallet = wallet(WalletStatus.ACTIVE);
		assertThrows(IllegalStateException.class, () -> walletService.reactivate(userWith(activeWallet)));
		assertThrows(IllegalStateException.class, () -> walletService.reactivate(new User()));

		verify(walletRepo, never()).save(frozenWallet);
		verify(walletRepo, never()).save(activeWallet);
	}

	private User userWith(Wallet wallet) {
		User user = new User();
		user.setWallet(wallet);
		return user;
	}

	private Wallet wallet(WalletStatus status) {
		Wallet wallet = new Wallet();
		wallet.setWalletStatus(status);
		wallet.setBalance(BigDecimal.ZERO);
		return wallet;
	}
}
