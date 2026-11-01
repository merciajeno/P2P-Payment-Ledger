package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.LoginRequest;
import com.mercia.dto.MeResponse;
import com.mercia.dto.RegisterRequest;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.AuthService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;
	
	@PostMapping("/login")
	public ResponseEntity<String> login(@Valid @RequestBody LoginRequest login)
	{
		return authService.login(login);
	}
	
	@PostMapping("/register")
	public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest register)
	{
		return authService.register(register);
	}
	
	@GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        User user = userRepository.findByName(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return ResponseEntity.ok(new MeResponse(user.getName(), user.getRole()));
    }
}
