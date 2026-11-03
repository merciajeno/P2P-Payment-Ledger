package com.mercia.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.dto.UserSearchResponse;
import com.mercia.enums.Role;
import com.mercia.enums.Status;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<UserSearchResponse> getFriends(String username) {
		return getUser(username).getFriends().stream()
				.map(friend -> new UserSearchResponse(friend.getName(), friend.getEmail()))
				.toList();
	}

	@Transactional
	public UserSearchResponse addFriend(String username, String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Friend email is required");
		}

		User user = getUser(username);
		User friend = userRepository.findByEmail(email.trim())
				.orElseThrow(() -> new UserNotFoundException("Friend account not found"));
		if (user.getId().equals(friend.getId())) {
			throw new IllegalArgumentException("You cannot add yourself as a friend");
		}
		if (friend.getRole() != Role.USER || friend.getStatus() != Status.ACTIVE) {
			throw new IllegalArgumentException("Only active users can be added as friends");
		}

		user.getFoes().remove(friend);
		user.getFriends().add(friend);
		userRepository.save(user);
		return new UserSearchResponse(friend.getName(), friend.getEmail());
	}

	@Transactional
	public void removeFriend(String username, String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Friend email is required");
		}

		User user = getUser(username);
		User friend = userRepository.findByEmail(email.trim())
				.orElseThrow(() -> new UserNotFoundException("Friend account not found"));
		if (user.getFriends().remove(friend)) {
			userRepository.save(user);
		}
	}

	private User getUser(String username) {
		return userRepository.findByName(username)
				.orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
	}
}
