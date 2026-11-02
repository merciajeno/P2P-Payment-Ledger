package com.mercia.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import com.mercia.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

	@Mock
	private JwtService jwtService;
	@Mock
	private UserDetailsService userDetailsService;
	@Mock
	private FilterChain filterChain;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void continuesWithoutAuthenticationWhenRequestHasNoCookies() throws IOException, ServletException {
		JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
		verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void continuesWithoutAuthenticationWhenTokenCookieIsAbsent() throws IOException, ServletException {
		JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account");
		request.setCookies(new Cookie("session", "unrelated"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void authenticatesFromTokenCookieAndContinuesFilterChain() throws IOException, ServletException {
		JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account");
		request.setCookies(new Cookie("session", "ignored"), new Cookie("token", "signed-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		UserDetails userDetails = User.withUsername("alice")
				.password("not-used")
				.authorities("ROLE_USER")
				.build();
		when(jwtService.extractUsername("signed-token")).thenReturn("alice");
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

		filter.doFilter(request, response, filterChain);

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		assertTrue(authentication.isAuthenticated());
		assertSame(userDetails, authentication.getPrincipal());
		assertEquals("alice", authentication.getName());
		assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
		verify(jwtService).extractUsername("signed-token");
		verify(userDetailsService).loadUserByUsername("alice");
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void continuesWithoutAuthenticationWhenTokenIsExpired() throws IOException, ServletException {
		JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/me");
		request.setCookies(new Cookie("token", "expired-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(jwtService.extractUsername("expired-token"))
				.thenThrow(new ExpiredJwtException(null, null, "Token expired"));

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
