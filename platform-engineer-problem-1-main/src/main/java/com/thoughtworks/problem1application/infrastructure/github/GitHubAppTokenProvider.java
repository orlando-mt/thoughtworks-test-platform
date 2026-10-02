package com.thoughtworks.problem1application.infrastructure.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.problem1application.configurations.PlatformProperties;

import io.jsonwebtoken.Jwts;

@Component
public class GitHubAppTokenProvider {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);

    private final PlatformProperties.Github github;
    private final RestClient restClient;

    private PrivateKey privateKey;
    private CachedToken cached;

    public GitHubAppTokenProvider(PlatformProperties properties) {
        this.github = properties.github();
        this.restClient = GitHubRestClients.create(github.apiUrl());
    }

    /** Token de instalación (1 hora de vida). Se renueva solo cuando está por vencer. */
    public synchronized String installationToken() {
        if (cached != null && Instant.now().isBefore(cached.expiresAt().minus(REFRESH_MARGIN))) {
            return cached.token();
        }
        InstallationTokenResponse response = restClient.post()
                .uri("/app/installations/{id}/access_tokens", github.installationId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appJwt())
                .retrieve()
                .body(InstallationTokenResponse.class);
        cached = new CachedToken(response.token(), response.expiresAt());
        return cached.token();
    }

    /** JWT firmado con la llave de la App: prueba ante GitHub "soy la App <appId>". Máximo 10 minutos. */
    private String appJwt() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(github.appId())
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(540)))
                .signWith(privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /** Se carga recién al primer uso: así las pruebas arrancan sin necesitar el .pem. */
    private PrivateKey privateKey() {
        if (privateKey == null) {
            privateKey = PemKeys.readPrivateKey(loadPem());
        }
        return privateKey;
    }

    private String loadPem() {
        if (github.privateKey() != null && !github.privateKey().isBlank()) {
            return github.privateKey();
        }
        if (github.privateKeyPath() == null || github.privateKeyPath().isBlank()) {
            throw new IllegalStateException("Set GITHUB_APP_PRIVATE_KEY or GITHUB_APP_PRIVATE_KEY_PATH");
        }
        try {
            return Files.readString(Path.of(github.privateKeyPath()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read GitHub App private key at " + github.privateKeyPath(), e);
        }
    }

    private record InstallationTokenResponse(String token, @JsonProperty("expires_at") Instant expiresAt) {
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}