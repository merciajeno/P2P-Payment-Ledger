package com.mercia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long>{

}
