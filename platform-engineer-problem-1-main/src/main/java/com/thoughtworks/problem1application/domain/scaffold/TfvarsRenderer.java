package com.thoughtworks.problem1application.domain.scaffold;

import java.util.Map;

/** Escribe environments/<env>.tfvars. Aquí no hay modelo: son datos, y los datos los pone el backend. */
public final class TfvarsRenderer {

    private TfvarsRenderer() {
    }

    /**
     * @param resourcesByVariable variable del módulo (su label) → nombre real del recurso → inputs
     */
    public static String render(String environment, Map<String, Map<String, Map<String, Object>>> resourcesByVariable) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Managed by the Kordanix platform: regenerated from its database on every request.\n");
        sb.append("environment = ").append(ScaffoldRequestFactory.hclLiteral(environment)).append('\n');

        resourcesByVariable.forEach((variable, resources) -> {
            sb.append('\n').append(variable).append(" = {\n");
            resources.forEach((resourceName, inputs) -> {
                sb.append("  ").append(ScaffoldRequestFactory.hclLiteral(resourceName));
                if (inputs.isEmpty()) {
                    sb.append(" = {}\n");
                    return;
                }
                sb.append(" = {\n");
                int width = inputs.keySet().stream().mapToInt(String::length).max().orElse(0);
                inputs.forEach((key, value) -> sb.append("    ")
                        .append(String.format("%-" + width + "s", key))
                        .append(" = ")
                        .append(ScaffoldRequestFactory.hclLiteral(value))
                        .append('\n'));
                sb.append("  }\n");
            });
            sb.append("}\n");
        });
        return sb.toString();
    }
}