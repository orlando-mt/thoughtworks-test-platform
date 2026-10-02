package com.thoughtworks.problem1application.application.dtos.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.thoughtworks.problem1application.domain.services.ResourceService.ResourceView;

public record ResourceResponseDTO(
        UUID id,
        String moduleId,
        String environment,
        String name,
        String physicalName,
        Map<String, Object> inputs,
        String status,
        String commitSha,
        String pipelineUrl,
        Instant createdAt) {

    public static ResourceResponseDTO from(ResourceView view) {
        return new ResourceResponseDTO(
                view.resource().getId(),
                view.resource().getModuleId(),
                view.resource().getEnvironment(),
                view.resource().getName(),
                view.resource().getPhysicalName(),
                view.inputs(),
                view.resource().getStatus().name(),
                view.resource().getCommitSha(),
                view.pipelineUrl(),
                view.resource().getCreatedAt());
    }
}