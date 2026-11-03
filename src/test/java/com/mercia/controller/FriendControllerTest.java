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

import com.mercia.dto.FriendRequest;
import com.mercia.dto.UserSearchResponse;
import com.mercia.service.FriendService;

@ExtendWith(MockitoExtension.class)
class FriendControllerTest {

	@Mock
	private FriendService friendService;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private FriendController controller;

	@Test
	void getFriendsReturnsFriendsForAuthenticatedUser() {
		UserSearchResponse friend = new UserSearchResponse("Bob", "bob@example.com");
		when(authentication.getName()).thenReturn("alice");
		when(friendService.getFriends("alice")).thenReturn(List.of(friend));

		var response = controller.getFriends(authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(List.of(friend), response.getBody());
		verify(friendService).getFriends("alice");
	}

	@ParameterizedTest
	@MethodSource("friendRequests")
	void addFriendDelegatesEmailAndReturnsAddedFriend(String email, UserSearchResponse expected) {
		when(authentication.getName()).thenReturn("alice");
		when(friendService.addFriend("alice", email)).thenReturn(expected);

		var response = controller.addFriend(new FriendRequest(email), authentication);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(expected, response.getBody());
		verify(friendService).addFriend("alice", email);
	}

	private static Stream<Arguments> friendRequests() {
		return Stream.of(
				arguments("bob@example.com", new UserSearchResponse("Bob", "bob@example.com")),
				arguments("carol@example.com", new UserSearchResponse("Carol", "carol@example.com")));
	}

	@Test
	void addFriendRejectsMissingRequest() {
		assertEquals("Friend email is required",
				assertThrows(IllegalArgumentException.class,
						() -> controller.addFriend(null, authentication)).getMessage());
	}

	@Test
	void removeFriendReturnsNoContentAndUsesAuthenticatedUser() {
		when(authentication.getName()).thenReturn("alice");

		var response = controller.removeFriend("bob@example.com", authentication);

		assertEquals(204, response.getStatusCode().value());
		verify(friendService).removeFriend("alice", "bob@example.com");
	}
}
