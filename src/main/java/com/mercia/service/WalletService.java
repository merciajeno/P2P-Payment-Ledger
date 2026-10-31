package com.mercia.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;

    @Transactional
    public void deactivate(User user) {
        Wallet wallet = getWallet(user);

        if (wallet.getWalletStatus() == WalletStatus.FROZEN) {
            throw new IllegalStateException("Wallet is frozen and cannot be changed");
        }
        if (wallet.getWalletStatus() == WalletStatus.INACTIVE) {
            throw new IllegalStateException("Wallet is already inactive");
        }

        wallet.setWalletStatus(WalletStatus.INACTIVE);
        walletRepo.save(wallet);
    }

    @Transactional
    public void reactivate(User user) {
        Wallet wallet = getWallet(user);

        if (wallet.getWalletStatus() == WalletStatus.FROZEN) {
            throw new IllegalStateException("Wallet is frozen and cannot be changed");
        }
        if (wallet.getWalletStatus() == WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is already active");
        }

        wallet.setWalletStatus(WalletStatus.ACTIVE);
        walletRepo.save(wallet);
    }

    private Wallet getWallet(User user) {
        Wallet wallet = user.getWallet();
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found");
        }
        return wallet;
    }
}
