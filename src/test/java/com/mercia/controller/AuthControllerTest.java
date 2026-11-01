package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.mercia.dto.LoginRequest;
import com.mercia.dto.MeResponse;
import com.mercia.dto.RegisterRequest;
import com.mercia.enums.Role;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private AuthService authService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private AuthController controller;

	@Test
	void loginDelegatesRequestToAuthService() {
		LoginRequest request = new LoginRequest("alice@example.com", "password");
		ResponseEntity<String> expected = ResponseEntity.ok("Login successful");
		when(authService.login(request)).thenReturn(expected);

		assertSame(expected, controller.login(request));

		verify(authService).login(request);
	}

	@Test
	void registerDelegatesRequestToAuthService() {
		RegisterRequest request = new RegisterRequest(
				"Alice", "alice@example.com", "+12345678901", "password123", "1234");
		ResponseEntity<String> expected = ResponseEntity.ok("Registered");
		when(authService.register(request)).thenReturn(expected);

		assertSame(expected, controller.register(request));

		verify(authService).register(request);
	}

	@Test
	void logoutExpiresAuthenticationCookie() {
		ResponseEntity<Void> response = controller.logout();

		assertEquals(204, response.getStatusCode().value());
		String setCookie = response.getHeaders().getFirst("Set-Cookie");
		assertTrue(setCookie.contains("token="));
		assertTrue(setCookie.contains("Path=/"));
		assertTrue(setCookie.contains("Max-Age=0"));
		assertTrue(setCookie.contains("HttpOnly"));
		assertTrue(setCookie.contains("SameSite=Strict"));
	}

	@Test
	void meReturnsAuthenticatedUserNameAndRole() {
		User user = new User();
		user.setName("alice");
		user.setRole(Role.USER);
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

		ResponseEntity<MeResponse> response = controller.me(authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(new MeResponse("alice", Role.USER), response.getBody());
		verify(userRepository).findByName("alice");
	}

	@Test
	void meThrowsWhenAuthenticatedUserCannotBeFound() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.me(authentication));
	}
}
