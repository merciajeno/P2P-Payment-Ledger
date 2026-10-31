package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

	private SecretKey secretKey;
	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		secretKey = Keys.hmacShaKeyFor(new byte[32]);
		jwtService = new JwtService(secretKey);
	}

	@Test
	void generateTokenCreatesSignedTokenWithUsernameAndThirtyMinuteExpiration() {
		long beforeGeneration = System.currentTimeMillis();

		String token = jwtService.generateToken("alice");

		var claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
		assertEquals("alice", claims.getSubject());
		assertTrue(claims.getIssuedAt().getTime() >= beforeGeneration - 1000);
		assertEquals(30 * 60 * 1000L,
				claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
	}

	@Test
	void extractUsernameReturnsSubjectForValidToken() {
		String token = jwtService.generateToken("alice");

		assertEquals("alice", jwtService.extractUsername(token));
	}

	@Test
	void extractUsernameRejectsTokenSignedWithAnotherKey() {
		SecretKey otherKey = Keys.hmacShaKeyFor(new byte[] {
				1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
				17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
		});
		String token = Jwts.builder().subject("alice").signWith(otherKey).compact();

		assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
	}

	@Test
	void extractUsernameRejectsMalformedToken() {
		assertThrows(JwtException.class, () -> jwtService.extractUsername("not-a-jwt"));
	}
}
