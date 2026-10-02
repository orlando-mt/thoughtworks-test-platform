package com.thoughtworks.problem1application.application.dtos.response;

import java.util.List;
import java.util.Map;

import com.thoughtworks.problem1application.domain.catalog.ModuleOutput;

/**
 * schema:   lo que se le puede preguntar al usuario
 * enforced: lo que la plataforma fija y nadie puede cambiar (el agente se lo puede contar al usuario)
 */
public record ModuleDetailDTO(
        String id,
        String displayName,
        String description,
        String source,
        String version,
        Map<String, Object> schema,
        Map<String, Object> enforced,
        List<ModuleOutput> outputs) {
}