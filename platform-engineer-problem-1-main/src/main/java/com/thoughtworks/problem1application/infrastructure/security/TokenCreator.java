package com.thoughtworks.problem1application.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenCreator {

	private final SecretKey key;
	private final Duration ttl;

	public TokenCreator(@Value("${jwt.secret}") String secret,
						@Value("${jwt.ttl-minutes:60}") long ttlMinutes) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.ttl = Duration.ofMinutes(ttlMinutes);
	}

	public String generateToken(String subject) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(subject)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttl)))
				.signWith(key)
				.compact();
	}

	/** Valida firma y expiración. Lanza JwtException si el token no es válido. */
	public Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}