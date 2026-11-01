package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mercia.dto.LoginRequest;
import com.mercia.dto.RegisterRequest;
import com.mercia.enums.Role;
import com.mercia.enums.Status;
import com.mercia.enums.WalletStatus;
import com.mercia.exception.InvalidPasswordException;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.model.Wallet;
import com.mercia.repository.UserRepository;
import com.mercia.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepo;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private WalletRepository walletRepo;
	@Mock
	private JwtService jwtService;
	@Mock
	private LoginAttemptService loginAttemptService;

	@InjectMocks
	private AuthService authService;

	private RegisterRequest registerRequest;

	@BeforeEach
	void setUp() {
		registerRequest = new RegisterRequest("alice", "alice@example.com", "+12345678901", "password123", "1234");
	}

	@Test
	void registerCreatesUserAndWalletWithEncodedCredentialsAndSafeDefaults() {
		when(userRepo.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
		when(userRepo.findByName(registerRequest.name())).thenReturn(Optional.empty());
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(passwordEncoder.encode("1234")).thenReturn("encoded-pin");

		var response = authService.register(registerRequest);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("User registered successfully", response.getBody());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
		verify(userRepo).save(userCaptor.capture());
		verify(walletRepo).save(walletCaptor.capture());

		User savedUser = userCaptor.getValue();
		Wallet savedWallet = walletCaptor.getValue();
		assertEquals("alice", savedUser.getName());
		assertEquals("alice@example.com", savedUser.getEmail());
		assertEquals("+12345678901", savedUser.getPhoneNumber());
		assertEquals("encoded-password", savedUser.getPassword());
		assertEquals("encoded-pin", savedUser.getPIN());
		assertEquals(Role.USER, savedUser.getRole());
		assertEquals(Status.ACTIVE, savedUser.getStatus());
		assertEquals(savedWallet, savedUser.getWallet());
		assertEquals(savedUser, savedWallet.getUser());
		assertEquals(BigDecimal.ZERO, savedWallet.getBalance());
		assertEquals(WalletStatus.ACTIVE, savedWallet.getWalletStatus());
	}

	@Test
	void registerRejectsAnExistingEmail() {
		when(userRepo.findByEmail(registerRequest.email())).thenReturn(Optional.of(new User()));
		when(userRepo.findByName(registerRequest.name())).thenReturn(Optional.empty());

		var response = authService.register(registerRequest);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("User already exists", response.getBody());
		verify(userRepo, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder, walletRepo);
	}

	@Test
	void registerRejectsAnExistingUsername() {
		when(userRepo.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
		when(userRepo.findByName(registerRequest.name())).thenReturn(Optional.of(new User()));

		var response = authService.register(registerRequest);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("User already exists", response.getBody());
		verify(userRepo, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder, walletRepo);
	}

	@Test
	void loginThrowsWhenEmailIsNotRegistered() {
		when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> authService.login(new LoginRequest("missing@example.com", "password123")));

		verifyNoInteractions(loginAttemptService, passwordEncoder, jwtService);
	}

	@Test
	void loginRejectsLockedAccountBeforeCheckingPassword() {
		User user = user("alice", "encoded-password");
		when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		when(loginAttemptService.isAccountLocked(user)).thenReturn(true);

		assertThrows(IllegalStateException.class,
				() -> authService.login(new LoginRequest("alice@example.com", "password123")));

		verifyNoInteractions(passwordEncoder, jwtService);
		verify(loginAttemptService, never()).recordFailedAttempt(user);
		verify(loginAttemptService, never()).recordSuccessfulLogin(user);
	}

	@Test
	void loginRecordsFailedAttemptWhenPasswordDoesNotMatch() {
		User user = user("alice", "encoded-password");
		when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		when(loginAttemptService.isAccountLocked(user)).thenReturn(false);
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		assertThrows(InvalidPasswordException.class,
				() -> authService.login(new LoginRequest("alice@example.com", "wrong-password")));

		verify(loginAttemptService).recordFailedAttempt(user);
		verify(loginAttemptService, never()).recordSuccessfulLogin(user);
		verifyNoInteractions(jwtService);
	}

	@Test
	void loginReturnsCookieAndRecordsSuccessfulLogin() {
		User user = user("alice", "encoded-password");
		when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		when(loginAttemptService.isAccountLocked(user)).thenReturn(false);
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(jwtService.generateToken("alice")).thenReturn("signed-token");

		var response = authService.login(new LoginRequest("alice@example.com", "password123"));

		assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
		assertEquals("Login successful", response.getBody());
		assertNotNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
		assertEquals(true, response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("token=signed-token"));
		assertEquals(true, response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("HttpOnly"));
		assertEquals(true, response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("SameSite=Strict"));
		verify(loginAttemptService).recordSuccessfulLogin(user);
		verify(jwtService).generateToken("alice");
	}

	private User user(String username, String password) {
		User user = new User();
		user.setName(username);
		user.setEmail(username + "@example.com");
		user.setPassword(password);
		return user;
	}
}
