package com.mercia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercia.model.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long>{

}
