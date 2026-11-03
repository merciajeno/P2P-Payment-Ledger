package com.mercia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mercia.filter.JwtFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,JwtFilter jwtFilter)
	{
		http.csrf(csrf->csrf.disable())
		.logout(logout -> logout.disable())
		.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
		.authorizeHttpRequests(auth->
		auth.requestMatchers("/login", "/register", "/logout").permitAll()
	    .requestMatchers("/wallet/**").hasRole("USER")
	    .requestMatchers("/api/ledger/**").hasRole("USER")
	    .requestMatchers("/transactions", "/transactions/**").hasRole("USER")
	    .requestMatchers("/users/stats").hasRole("USER")
	    .requestMatchers("/users/search").hasRole("USER")
	    .requestMatchers("/users/friends").hasRole("USER")
	    .requestMatchers("/users/foes", "/users/foes/**").hasRole("USER")
	    .requestMatchers("/notifications/**").authenticated()
	    .requestMatchers("/admin/**").hasRole("ADMIN")
	    .requestMatchers("/me").authenticated()
	    .anyRequest().authenticated());

		return http.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder()
	{
		return new BCryptPasswordEncoder();
	}
}
