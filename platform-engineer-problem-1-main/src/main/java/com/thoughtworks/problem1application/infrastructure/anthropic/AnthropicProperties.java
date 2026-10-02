package com.thoughtworks.problem1application.infrastructure.anthropic;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("platform.anthropic")
public record AnthropicProperties(String apiUrl, String apiKey, String modelId, int maxTokens, Duration timeout) {
}