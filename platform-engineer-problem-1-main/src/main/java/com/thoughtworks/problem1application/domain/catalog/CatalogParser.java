package com.thoughtworks.problem1application.domain.catalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.problem1application.domain.exceptions.InvalidCatalogException;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Component
public class CatalogParser {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public List<ModuleDefinition> parse(CatalogSource source) {
        CatalogFile catalog = YAML.readValue(source.read("catalog.yml"), CatalogFile.class);
        List<String> problems = new ArrayList<>();
        List<ModuleDefinition> modules = new ArrayList<>();

        for (CatalogEntry entry : catalog.modules()) {
            TerraformDocs docs = JSON.readValue(source.read("generated/" + entry.id() + ".json"), TerraformDocs.class);
            modules.add(toDefinition(entry, docs, problems));
        }
        if (!problems.isEmpty()) {
            throw new InvalidCatalogException(problems);
        }
        return modules;
    }

    private ModuleDefinition toDefinition(CatalogEntry entry, TerraformDocs docs, List<String> problems) {
        String prefix = entry.id() + ": ";
        Map<String, TfInput> inputs = docs.inputs().stream()
                .collect(Collectors.toMap(TfInput::name, Function.identity()));
        Set<String> outputNames = docs.outputs().stream().map(TfOutput::name).collect(Collectors.toSet());
        Set<String> covered = new HashSet<>();

        // La variable del nombre debe existir y ser string
        TfInput nameInput = inputs.get(entry.nameVariable());
        if (nameInput == null || !"string".equals(nameInput.type())) {
            problems.add(prefix + "name_variable '" + entry.nameVariable() + "' must be a string input of the module");
        }
        covered.add(entry.nameVariable());

        if (entry.tagsVariable() != null) {
            if (!inputs.containsKey(entry.tagsVariable())) {
                problems.add(prefix + "tags_variable '" + entry.tagsVariable() + "' is not an input of the module");
            }
            covered.add(entry.tagsVariable());
        }

        // Lo que se pregunta: debe existir y ser de tipo simple
        List<ModuleInput> asked = new ArrayList<>();
        for (String name : entry.ask()) {
            TfInput input = inputs.get(name);
            if (input == null) {
                problems.add(prefix + "asked input '" + name + "' is not an input of the module");
                continue;
            }
            InputType type = InputType.fromTerraform(input.type());
            if (type == null) {
                problems.add(prefix + "asked input '" + name + "' has a complex type (" + input.type()
                        + ") and cannot be asked to a user");
                continue;
            }
            asked.add(new ModuleInput(name, type, input.description(), input.defaultValue(), input.required()));
            covered.add(name);
        }

        // Lo que fija la plataforma: debe existir
        for (String name : entry.fixed().keySet()) {
            if (!inputs.containsKey(name)) {
                problems.add(prefix + "fixed input '" + name + "' is not an input of the module");
            }
            covered.add(name);
        }

        // Toda variable requerida del módulo debe quedar cubierta; si no, terraform plan fallaría
        inputs.values().stream()
                .filter(TfInput::required)
                .filter(input -> !covered.contains(input.name()))
                .forEach(input -> problems.add(prefix + "required input '" + input.name()
                        + "' is not covered by name_variable, ask or fixed"));

        List<ModuleOutput> outputs = new ArrayList<>();
        for (String name : entry.outputs()) {
            if (!outputNames.contains(name)) {
                problems.add(prefix + "output '" + name + "' is not an output of the module");
                continue;
            }
            docs.outputs().stream()
                    .filter(output -> output.name().equals(name))
                    .findFirst()
                    .ifPresent(output -> outputs.add(new ModuleOutput(output.name(), output.description())));
        }

        return new ModuleDefinition(entry.id(), entry.displayName(), entry.description(), entry.source(),
                entry.version(), entry.nameVariable(), entry.tagsVariable(), asked, entry.fixed(), outputs);
    }

    // ---------- formato de catalog.yml ----------

    record CatalogFile(List<CatalogEntry> modules) {
    }

    record CatalogEntry(String id, String displayName, String description, String source, String version,
                        String nameVariable, String tagsVariable, List<String> ask, Map<String, Object> fixed,
                        List<String> outputs) {

        CatalogEntry {
            ask = ask == null ? List.of() : ask;
            fixed = fixed == null ? Map.of() : fixed;
            outputs = outputs == null ? List.of() : outputs;
        }
    }

    // ---------- formato de terraform-docs json ----------

    record TerraformDocs(List<TfInput> inputs, List<TfOutput> outputs) {

        TerraformDocs {
            inputs = inputs == null ? List.of() : inputs;
            outputs = outputs == null ? List.of() : outputs;
        }
    }

    record TfInput(String name, String type, String description, @JsonProperty("default") Object defaultValue,
                   boolean required) {
    }

    record TfOutput(String name, String description) {
    }
}