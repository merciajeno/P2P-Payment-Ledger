package com.mercia.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.FoeRequest;
import com.mercia.dto.UserSearchResponse;
import com.mercia.service.FoeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/foes")
@RequiredArgsConstructor
public class FoeController {

	private final FoeService foeService;

	@GetMapping
	public ResponseEntity<List<UserSearchResponse>> getFoes(Authentication authentication) {
		return ResponseEntity.ok(foeService.getFoes(authentication.getName()));
	}

	@PostMapping
	public ResponseEntity<UserSearchResponse> addFoe(
			@RequestBody FoeRequest request,
			Authentication authentication) {
		if (request == null) {
			throw new IllegalArgumentException("Foe email is required");
		}
		return ResponseEntity.ok(foeService.addFoe(authentication.getName(), request.email()));
	}

	@DeleteMapping
	public ResponseEntity<Void> removeFoe(
			@RequestParam String email,
			Authentication authentication) {
		foeService.removeFoe(authentication.getName(), email);
		return ResponseEntity.noContent().build();
	}
}
