package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.mercia.model.User;
import com.mercia.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserController controller;

	@Test
	void searchReturnsUserDetailsWhenMatchExists() {
		User user = new User();
		user.setName("Alice");
		user.setEmail("alice@example.com");
		when(userRepository.findByEmailOrPhone("alice@example.com")).thenReturn(Optional.of(user));

		var response = controller.search("alice@example.com");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Alice", response.getBody().name());
		assertEquals("alice@example.com", response.getBody().email());
		verify(userRepository).findByEmailOrPhone("alice@example.com");
	}

	@Test
	void searchReturnsNotFoundWhenThereIsNoMatch() {
		when(userRepository.findByEmailOrPhone("unknown@example.com")).thenReturn(Optional.empty());

		var response = controller.search("unknown@example.com");

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}
}
