package com.mercia.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CustomUserService implements UserDetailsService{

	private final UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByName(username)
				.orElseThrow(() -> new UserNotFoundException("User name:" + username + " not found"));
		return org.springframework.security.core.userdetails.User.builder().username(username)
				.password(user.getPassword()).roles(user.getRole().toString()).build();
	}

}
