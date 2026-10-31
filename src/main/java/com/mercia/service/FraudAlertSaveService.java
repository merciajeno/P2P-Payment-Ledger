package com.mercia.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.FraudAlert;
import com.mercia.model.FraudAlertType;
import com.mercia.model.Role;
import com.mercia.model.Transaction;
import com.mercia.model.Wallet;
import com.mercia.repository.FraudAlertRepository;
import com.mercia.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FraudAlertSaveService {

	private final FraudAlertRepository fraudAlertRepo;
	private final UserRepository userRepo;
	private final NotificationService notificationService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(Wallet wallet, Transaction transaction, FraudAlertType type, BigDecimal amount,
			String description) {

		FraudAlert alert = new FraudAlert();
		alert.setWallet(wallet);
		alert.setTransaction(transaction);
		alert.setAlertType(type);
		alert.setAmount(amount);
		alert.setDescription(description);
		fraudAlertRepo.save(alert);

		userRepo.findByRole(Role.ADMIN).forEach(admin -> notificationService.send(admin,
				"Fraud alert raised: " + type.name() + " — ₹" + amount + " by wallet #" + wallet.getId()));

	}
}
