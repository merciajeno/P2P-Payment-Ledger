package com.mercia.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.LoginAttempt;

public interface LoginAttemptRepository
        extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findByUserId(Long userId);

    List<LoginAttempt> findByLockedTrueAndLockedUntilLessThanEqual(
            Instant currentTime
    );
}