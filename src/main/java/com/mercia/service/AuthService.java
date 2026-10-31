package com.mercia.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mercia.dto.LoginRequest;
import com.mercia.dto.RegisterRequest;
import com.mercia.exception.InvalidPasswordException;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.Role;
import com.mercia.model.Status;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.model.WalletStatus;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final WalletRepository walletRepo;
	private final JwtService jwtService;
	private final LoginAttemptService loginAttemptService;

	@Transactional
	public ResponseEntity<String> register(RegisterRequest request) {

		Optional<User> byEmail = userRepo.findByEmail(request.email());
		Optional<User> byUsername = userRepo.findByName(request.name());

		if (byEmail.isPresent() || byUsername.isPresent()) {
			return ResponseEntity.badRequest().body("User already exists");
		}

		User user = new User();
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal(0));
		wallet.setWalletStatus(WalletStatus.ACTIVE);
		wallet.setUser(user);
		user.setWallet(wallet);

		user.setName(request.name());
		user.setEmail(request.email());
		user.setPhoneNumber(request.phoneNumber());

		// Never store the raw password
		user.setPassword(passwordEncoder.encode(request.password()));

		// Hash the transaction PIN as well
		user.setPIN(passwordEncoder.encode(request.transactionPin()));

		// Assign safe defaults on the server
		user.setRole(Role.USER);
		user.setStatus(Status.ACTIVE);

		userRepo.save(user);
		walletRepo.save(wallet);

		log.debug("User registered successfully with id: {}", user.getId());

		return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
	}

	@Transactional
	public ResponseEntity<String> login(LoginRequest login) {
		String email = login.email();
		String password = login.password();
		User existingUser = userRepo.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("Email is not registered"));
		
		if(loginAttemptService.isAccountLocked(existingUser))
		{
			 throw new IllegalStateException("Account is locked");
		}

		boolean passwordMatch = passwordEncoder.matches(password, existingUser.getPassword());
		if (!passwordMatch) {
			log.warn("Incorrect password");
			loginAttemptService.recordFailedAttempt(existingUser);
			throw new InvalidPasswordException("Invalid password");
		}
		loginAttemptService.recordSuccessfulLogin(existingUser);
		String username = existingUser.getName();
		String token = jwtService.generateToken(username);
		ResponseCookie cookie = ResponseCookie.from("token", token).httpOnly(true).secure(false)
				.path("/").sameSite("Strict").maxAge(Duration.ofMinutes(30)).build();
		return ResponseEntity.accepted().header(HttpHeaders.SET_COOKIE, cookie.toString()).body("Login successful");
	}
}
