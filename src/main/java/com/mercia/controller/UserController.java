package com.mercia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.UserSearchResponse;
import com.mercia.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Search by email or phone number
    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> search(
            @RequestParam String query) {

        return userRepository.findByEmailOrPhone(query)
                .map(u -> ResponseEntity.ok(
                        new UserSearchResponse(u.getName(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }
}
