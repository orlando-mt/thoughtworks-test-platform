package com.thoughtworks.problem1application.application.dtos.response;

import java.util.List;
import java.util.Map;

import com.thoughtworks.problem1application.domain.services.ProjectService.ModulePreparation;

public record PrepareProjectResponseDTO(
        String project,
        String repoUrl,
        String moduleId,
        List<String> modules,
        boolean generated,
        int attempts,
        Map<String, String> files) {

    public static PrepareProjectResponseDTO from(ModulePreparation preparation) {
        return new PrepareProjectResponseDTO(
                preparation.project().getName(),
                preparation.project().getRepoUrl(),
                preparation.moduleId(),
                List.copyOf(preparation.project().getModules()),
                preparation.attempts() > 0,
                preparation.attempts(),
                preparation.files());
    }
}