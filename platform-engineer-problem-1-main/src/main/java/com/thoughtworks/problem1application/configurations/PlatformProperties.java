package com.thoughtworks.problem1application.configurations;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
        List<String> environments,
        List<String> selfServiceEnvironments,
        String namePattern,
        @DefaultValue("5m") Duration catalogTtl,
        Github github) {

    public record Github(
            @DefaultValue("https://api.github.com") String apiUrl,
            String org,
            String appId,
            String installationId,
            String privateKey,
            String privateKeyPath,
            @DefaultValue("platform-catalog") String catalogRepo,
            @DefaultValue("main") String catalogRef,
            @DefaultValue("infra-template") String templateRepo,
            @DefaultValue("-infra") String repoSuffix) {
    }
}