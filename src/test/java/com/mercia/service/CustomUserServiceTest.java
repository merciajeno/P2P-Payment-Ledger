package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import com.mercia.enums.Role;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserService customUserService;

	@Test
	void loadUserByUsernameReturnsSpringSecurityUserDetails() {
		User user = new User();
		user.setName("alice");
		user.setPassword("encoded-password");
		user.setRole(Role.ADMIN);
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserService.loadUserByUsername("alice");

		assertEquals("alice", userDetails.getUsername());
		assertEquals("encoded-password", userDetails.getPassword());
		assertEquals("ROLE_ADMIN", userDetails.getAuthorities().iterator().next().getAuthority());
		verify(userRepository).findByName("alice");
	}

	@Test
	void loadUserByUsernameThrowsWhenUserDoesNotExist() {
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		UserNotFoundException exception = assertThrows(UserNotFoundException.class,
				() -> customUserService.loadUserByUsername("missing"));

		assertEquals("User name:missing not found", exception.getMessage());
		verify(userRepository).findByName("missing");
	}
}
