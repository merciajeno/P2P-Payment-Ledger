package com.mercia.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.mercia.model.Status;
import com.mercia.model.Transaction;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.TransactionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TransactionService {

	private final TransactionRepository transactionRepo;
	
	public void makeTransaction(User sender,User receiver,BigDecimal amount)
	{
		if(receiver.getStatus()!=Status.ACTIVE)// the sender is revoked
		{
			
		}
		
		Wallet senderWallet = sender.getWallet();
		Wallet receiverWallet = receiver.getWallet();
		senderWallet.getBalance().subtract(amount);
		receiverWallet.getBalance().add(amount);
		
		Transaction transaction = new Transaction();
		transaction.setAmount(amount);
	    transactionRepo.save(transaction);
	}
}
