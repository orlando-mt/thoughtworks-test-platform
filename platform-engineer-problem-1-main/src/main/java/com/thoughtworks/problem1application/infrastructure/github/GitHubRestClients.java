package com.thoughtworks.problem1application.infrastructure.github;

import org.springframework.web.client.RestClient;

final class GitHubRestClients {

    private GitHubRestClients() {
    }

    static RestClient create(String apiUrl) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}