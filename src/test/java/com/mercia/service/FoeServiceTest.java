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

import com.mercia.dto.UserSearchResponse;
import com.mercia.enums.Role;
import com.mercia.enums.Status;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FoeServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private FoeService foeService;

	@Test
	void addFoeReplacesFriendRelationshipOnlyForTheCurrentUser() {
		User user = user(1L, "alice", "alice@example.com");
		User foe = user(2L, "bob", "bob@example.com");
		user.getFriends().add(foe);
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));
		when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(foe));

		assertEquals(new UserSearchResponse("bob", "bob@example.com"),
				foeService.addFoe("alice", "bob@example.com"));

		assertEquals(0, user.getFriends().size());
		assertEquals(1, user.getFoes().size());
		assertEquals(0, foe.getFoes().size());
		verify(userRepository).save(user);
	}

	@Test
	void addFoeRejectsSelfAndInactiveUsers() {
		User user = user(1L, "alice", "alice@example.com");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));
		when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		assertThrows(IllegalArgumentException.class,
				() -> foeService.addFoe("alice", "alice@example.com"));

		User inactive = user(2L, "inactive", "inactive@example.com");
		inactive.setStatus(Status.INACTIVE);
		when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactive));
		assertThrows(IllegalArgumentException.class,
				() -> foeService.addFoe("alice", "inactive@example.com"));
	}

	private User user(Long id, String name, String email) {
		User user = new User();
		user.setId(id);
		user.setName(name);
		user.setEmail(email);
		user.setRole(Role.USER);
		user.setStatus(Status.ACTIVE);
		return user;
	}
}
