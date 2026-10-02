package com.thoughtworks.problem1application.application.dtos.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.problem1application.domain.entities.Project;

public record ProjectResponseDTO(UUID id, String name, String repoName, String repoUrl, List<String> modules,
                                 Instant createdAt) {

    public static ProjectResponseDTO from(Project project) {
        return new ProjectResponseDTO(project.getId(), project.getName(), project.getRepoName(),
                project.getRepoUrl(), List.copyOf(project.getModules()), project.getCreatedAt());
    }
}