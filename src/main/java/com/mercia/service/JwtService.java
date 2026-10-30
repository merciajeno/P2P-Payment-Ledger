package com.mercia.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class JwtService {

	private final SecretKey secretKey;
	
	public String generateToken(String username) {

		long now = System.currentTimeMillis();
		return Jwts.builder().subject(username).issuedAt(new Date(now)).expiration(new Date(now + 1000 * 60 * 30))// 30
																													// minutes
				.signWith(secretKey).compact();
	}

	public String extractUsername(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
	}
}
