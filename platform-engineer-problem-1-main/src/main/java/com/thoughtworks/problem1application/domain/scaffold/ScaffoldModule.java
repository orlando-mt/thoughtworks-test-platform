package com.thoughtworks.problem1application.domain.scaffold;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lo que el modelo necesita saber de un módulo del catálogo.
 * El label es el contrato con el tfvars: nombre del bloque module y de su variable.
 */
public record ScaffoldModule(
        String id,
        String label,
        String description,
        String source,
        String nameVariable,
        String tagsVariable,
        List<String> inputs,
        String inputsSchema,
        Map<String, String> fixed,
        List<String> outputs) {

    public static String labelOf(String moduleId) {
        return moduleId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
}