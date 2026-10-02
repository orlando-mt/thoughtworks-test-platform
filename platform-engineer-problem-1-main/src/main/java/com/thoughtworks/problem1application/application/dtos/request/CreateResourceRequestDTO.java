package com.thoughtworks.problem1application.application.dtos.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record CreateResourceRequestDTO(
        @NotBlank String moduleId,
        @NotBlank String environment,
        @NotBlank String name,
        Map<String, Object> inputs) {
}