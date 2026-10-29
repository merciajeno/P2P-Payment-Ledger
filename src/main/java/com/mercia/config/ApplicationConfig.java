package com.mercia.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;

@Configuration
public class ApplicationConfig {

	@Value("${secret.key}")
	private String secret;
	
	@Bean
	String secret()
	{
		return secret;
	}

	@Bean
	SecretKey secretKey(String secret) {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
}
