package com.thoughtworks.problem1application.domain.scaffold;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.thoughtworks.problem1application.domain.catalog.ModuleCatalog;
import com.thoughtworks.problem1application.domain.catalog.ModuleDefinition;
import com.thoughtworks.problem1application.domain.catalog.ModuleInput;
import com.thoughtworks.problem1application.domain.catalog.ModuleOutput;

import tools.jackson.databind.json.JsonMapper;

/** Traduce los módulos del catálogo al contrato que recibe el modelo. */
@Component
public class ScaffoldRequestFactory {

    private final ModuleCatalog catalog;
    private final JsonMapper jsonMapper;
    private final TerraformProperties terraform;
    private final List<String> environments;

    public ScaffoldRequestFactory(ModuleCatalog catalog,
                                  JsonMapper jsonMapper,
                                  TerraformProperties terraform,
                                  @Value("${platform.environments}") List<String> environments) {
        this.catalog = catalog;
        this.jsonMapper = jsonMapper;
        this.terraform = terraform;
        this.environments = List.copyOf(environments);
    }

    public ScaffoldRequest build(String project, String repoName, Collection<String> moduleIds) {
        List<ScaffoldModule> modules = moduleIds.stream().map(this::toScaffoldModule).toList();
        return new ScaffoldRequest(project, repoName, terraform.region(), terraform.stateBucket(),
                terraform.awsProviderVersion(), environments, modules);
    }

    private ScaffoldModule toScaffoldModule(String moduleId) {
        ModuleDefinition def = catalog.get(moduleId);
        Map<String, Object> enforced = def.enforced() == null ? Map.of() : def.enforced();
        List<ModuleInput> allInputs = def.inputs() == null ? List.of() : def.inputs();
        List<ModuleOutput> allOutputs = def.outputs() == null ? List.of() : def.outputs();

        // Lo que pone la plataforma (nombre, tags, políticas) no es input del usuario
        Set<String> platformOwned = new HashSet<>(enforced.keySet());
        platformOwned.add(def.nameVariable());
        if (def.tagsVariable() != null) {
            platformOwned.add(def.tagsVariable());
        }
        List<ModuleInput> asked = allInputs.stream()
                .filter(input -> !platformOwned.contains(input.name()))
                .toList();

        Map<String, String> fixed = new LinkedHashMap<>();
        enforced.forEach((name, value) -> fixed.put(name, hclLiteral(value)));

        return new ScaffoldModule(
                def.id(),
                ScaffoldModule.labelOf(def.id()),
                def.description(),
                def.source() + "?ref=" + def.version(),
                def.nameVariable(),
                def.tagsVariable(),
                asked.stream().map(ModuleInput::name).toList(),
                jsonMapper.writeValueAsString(inputsSchema(asked)),
                fixed,
                allOutputs.stream().map(ModuleOutput::name).toList());
    }

    /** Tipo, descripción y default de cada input, para que el modelo arme el object() de la variable. */
    private static Map<String, Object> inputsSchema(List<ModuleInput> inputs) {
        Map<String, Object> schema = new LinkedHashMap<>();
        for (ModuleInput input : inputs) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", input.type().jsonType());
            if (input.description() != null) {
                property.put("description", input.description());
            }
            if (input.defaultValue() != null) {
                property.put("default", input.defaultValue());
            }
            property.put("required", input.required());
            schema.put(input.name(), property);
        }
        return schema;
    }

    public static String hclLiteral(Object value) {
        return switch (value) {
            case null -> "null";
            case Boolean b -> b.toString();
            case Number n -> n.toString();
            default -> "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        };
    }
}