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
public class FoeService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<UserSearchResponse> getFoes(String username) {
		return getUser(username).getFoes().stream()
				.map(foe -> new UserSearchResponse(foe.getName(), foe.getEmail()))
				.toList();
	}

	@Transactional
	public UserSearchResponse addFoe(String username, String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Foe email is required");
		}

		User user = getUser(username);
		User foe = userRepository.findByEmail(email.trim())
				.orElseThrow(() -> new UserNotFoundException("Foe account not found"));
		if (user.getId().equals(foe.getId())) {
			throw new IllegalArgumentException("You cannot add yourself as a foe");
		}
		if (foe.getRole() != Role.USER || foe.getStatus() != Status.ACTIVE) {
			throw new IllegalArgumentException("Only active users can be added as foes");
		}

		user.getFriends().remove(foe);
		user.getFoes().add(foe);
		userRepository.save(user);
		return new UserSearchResponse(foe.getName(), foe.getEmail());
	}

	@Transactional
	public void removeFoe(String username, String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Foe email is required");
		}

		User user = getUser(username);
		User foe = userRepository.findByEmail(email.trim())
				.orElseThrow(() -> new UserNotFoundException("Foe account not found"));
		if (user.getFoes().remove(foe)) {
			userRepository.save(user);
		}
	}

	private User getUser(String username) {
		return userRepository.findByName(username)
				.orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
	}
}
