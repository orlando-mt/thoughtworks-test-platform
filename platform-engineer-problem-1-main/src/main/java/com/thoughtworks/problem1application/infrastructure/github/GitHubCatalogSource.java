package com.thoughtworks.problem1application.infrastructure.github;

import org.springframework.stereotype.Component;

import com.thoughtworks.problem1application.configurations.PlatformProperties;
import com.thoughtworks.problem1application.domain.catalog.CatalogSource;

@Component
public class GitHubCatalogSource implements CatalogSource {

    private final GitHubClient gitHub;
    private final PlatformProperties.Github config;

    public GitHubCatalogSource(GitHubClient gitHub, PlatformProperties properties) {
        this.gitHub = gitHub;
        this.config = properties.github();
    }

    @Override
    public String read(String path) {
        return gitHub.readRawFile(config.catalogRepo(), path, config.catalogRef());
    }
}