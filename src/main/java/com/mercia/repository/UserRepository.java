package com.mercia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mercia.model.Role;
import com.mercia.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByName(String name);

	Optional<User> findByEmail(String email);

	@Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :start AND u.createdAt < :end")
	long countUsersRegisteredBetween(@Param("start") Instant start, @Param("end") Instant end);

	// In UserRepository
	List<User> findByRole(Role role);

	@Query("""
			SELECT u FROM User u
			WHERE LOWER(u.email) = LOWER(:query)
			OR u.phoneNumber = :query
			""")
	Optional<User> findByEmailOrPhone(@Param("query") String query);

}
