package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.mercia.dto.FoeRequest;
import com.mercia.dto.UserSearchResponse;
import com.mercia.service.FoeService;

@ExtendWith(MockitoExtension.class)
class FoeControllerTest {

	@Mock
	private FoeService foeService;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private FoeController controller;

	@Test
	void getFoesReturnsFoesForAuthenticatedUser() {
		UserSearchResponse foe = new UserSearchResponse("Bob", "bob@example.com");
		when(authentication.getName()).thenReturn("alice");
		when(foeService.getFoes("alice")).thenReturn(List.of(foe));

		var response = controller.getFoes(authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(List.of(foe), response.getBody());
		verify(foeService).getFoes("alice");
	}

	@ParameterizedTest
	@MethodSource("foeRequests")
	void addFoeDelegatesEmailAndReturnsAddedFoe(String email, UserSearchResponse expected) {
		when(authentication.getName()).thenReturn("alice");
		when(foeService.addFoe("alice", email)).thenReturn(expected);

		var response = controller.addFoe(new FoeRequest(email), authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(expected, response.getBody());
		verify(foeService).addFoe("alice", email);
	}

	private static Stream<Arguments> foeRequests() {
		return Stream.of(
				arguments("bob@example.com", new UserSearchResponse("Bob", "bob@example.com")),
				arguments("carol@example.com", new UserSearchResponse("Carol", "carol@example.com")));
	}

	@Test
	void addFoeRejectsMissingRequest() {
		assertEquals("Foe email is required",
				assertThrows(IllegalArgumentException.class,
						() -> controller.addFoe(null, authentication)).getMessage());
	}

	@Test
	void removeFoeReturnsNoContentAndUsesAuthenticatedUser() {
		when(authentication.getName()).thenReturn("alice");

		var response = controller.removeFoe("bob@example.com", authentication);

		assertEquals(204, response.getStatusCode().value());
		verify(foeService).removeFoe("alice", "bob@example.com");
	}
}
