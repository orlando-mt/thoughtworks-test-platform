package com.thoughtworks.problem1application.domain.catalog;

import java.util.List;
import java.util.Map;

/** Un módulo publicado: datos del catalog.yml unidos con lo que terraform-docs extrajo del módulo. */
public record ModuleDefinition(
        String id,
        String displayName,
        String description,
        String source,
        String version,
        String nameVariable,
        String tagsVariable,
        List<ModuleInput> inputs,
        Map<String, Object> enforced,
        List<ModuleOutput> outputs) {
}