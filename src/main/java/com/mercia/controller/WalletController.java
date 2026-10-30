package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.WalletBalanceResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

	private final UserRepository userRepository;

	@GetMapping("/balance")
	public ResponseEntity<WalletBalanceResponse> getBalance(Authentication authentication) {
		User user = userRepository.findByName(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
		if (user.getWallet() == null || user.getWallet().getBalance() == null) {
			throw new IllegalStateException("Wallet balance unavailable for authenticated user");
		}

		return ResponseEntity.ok(new WalletBalanceResponse(user.getWallet().getBalance()));
	}
}
