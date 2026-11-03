package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.UserStatsResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.service.UserStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/stats")
@RequiredArgsConstructor
public class UserStatsController {

	private final UserRepository userRepository;
	private final UserStatsService userStatsService;

	@GetMapping
	public ResponseEntity<UserStatsResponse> getStats(Authentication authentication) {
		User user = userRepository.findByName(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
		Wallet wallet = user.getWallet();
		if (wallet == null) {
			throw new IllegalStateException("Wallet not found");
		}

		return ResponseEntity.ok(userStatsService.getStats(wallet.getId()));
	}
}
