package com.thoughtworks.problem1application.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sin @Component: se registra solo en JwtConfig, con las rutas que protege.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final TokenCreator tokenCreator;

	public JwtAuthenticationFilter(TokenCreator tokenCreator) {
		this.tokenCreator = tokenCreator;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			chain.doFilter(request, response);
			return;
		}

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			unauthorized(response, "Missing bearer token.");
			return;
		}

		try {
			Claims claims = tokenCreator.parse(header.substring(BEARER.length()));
			request.setAttribute("claims", claims);
		} catch (JwtException | IllegalArgumentException e) {
			unauthorized(response, "Invalid or expired token.");
			return;
		}

		chain.doFilter(request, response);
	}

	private void unauthorized(HttpServletResponse response, String detail) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/problem+json");
		response.getWriter().write(
				"{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + detail + "\"}");
	}
}