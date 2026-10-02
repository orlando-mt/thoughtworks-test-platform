package com.thoughtworks.problem1application.infrastructure.bedrock;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("platform.bedrock")
public record BedrockProperties(String region, String modelId, String profile, int maxTokens, Duration timeout) {
}