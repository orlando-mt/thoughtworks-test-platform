package com.thoughtworks.problem1application.domain.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thoughtworks.problem1application.configurations.PlatformProperties;
import com.thoughtworks.problem1application.domain.exceptions.CatalogUnavailableException;
import com.thoughtworks.problem1application.domain.exceptions.ModuleNotFoundException;

@Service
public class ModuleCatalog {

    private static final Logger log = LoggerFactory.getLogger(ModuleCatalog.class);

    private final CatalogSource source;
    private final CatalogParser parser;
    private final PlatformProperties properties;

    private volatile Snapshot snapshot;

    public ModuleCatalog(CatalogSource source, CatalogParser parser, PlatformProperties properties) {
        this.source = source;
        this.parser = parser;
        this.properties = properties;
    }

    public Collection<ModuleDefinition> all() {
        return current().modules().values();
    }

    public ModuleDefinition get(String id) {
        ModuleDefinition module = current().modules().get(id);
        if (module == null) {
            throw new ModuleNotFoundException(id);
        }
        return module;
    }

    /** JSON Schema del pedido para crear una instancia del módulo: nombre, entorno e inputs preguntables. */
    public Map<String, Object> schemaFor(ModuleDefinition module) {
        Map<String, Object> inputProperties = new LinkedHashMap<>();
        List<String> requiredInputs = new ArrayList<>();
        for (ModuleInput input : module.inputs()) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", input.type().jsonType());
            putIfPresent(property, "description", input.description());
            putIfPresent(property, "default", input.defaultValue());
            inputProperties.put(input.name(), property);
            if (input.required()) {
                requiredInputs.add(input.name());
            }
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("type", "object");
        inputs.put("properties", inputProperties);
        inputs.put("required", requiredInputs);
        inputs.put("additionalProperties", false);

        Map<String, Object> name = new LinkedHashMap<>();
        name.put("type", "string");
        name.put("pattern", properties.namePattern());
        name.put("description", "Short name (lowercase letters, numbers, hyphens). The platform builds the real "
                + "resource name as <project>-<environment>-<name>-<suffix>.");

        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("type", "string");
        environment.put("enum", properties.environments());
        environment.put("description", "Self-service environments: " + properties.selfServiceEnvironments()
                + ". Other environments require the platform team.");

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("name", name);
        rootProperties.put("environment", environment);
        rootProperties.put("inputs", inputs);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("required", List.of("name", "environment"));
        schema.put("properties", rootProperties);
        schema.put("additionalProperties", false);
        return schema;
    }

    /** Recarga si la versión en memoria venció. Si la recarga falla, sigue sirviendo la anterior. */
    private Snapshot current() {
        Snapshot current = snapshot;
        if (current != null && !isStale(current)) {
            return current;
        }
        synchronized (this) {
            current = snapshot;
            if (current != null && !isStale(current)) {
                return current;
            }
            try {
                snapshot = load();
            } catch (RuntimeException e) {
                if (current == null) {
                    throw new CatalogUnavailableException(e);
                }
                log.warn("Catalog refresh failed, serving the previous version: {}", e.getMessage());
                // Reinicia el reloj para no reintentar en cada petición
                snapshot = new Snapshot(current.modules(), Instant.now());
            }
            return snapshot;
        }
    }

    private boolean isStale(Snapshot current) {
        return current.loadedAt().plus(properties.catalogTtl()).isBefore(Instant.now());
    }

    private Snapshot load() {
        Map<String, ModuleDefinition> modules = new LinkedHashMap<>();
        for (ModuleDefinition module : parser.parse(source)) {
            modules.put(module.id(), module);
        }
        log.info("Catalog loaded: {}", modules.keySet());
        return new Snapshot(modules, Instant.now());
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private record Snapshot(Map<String, ModuleDefinition> modules, Instant loadedAt) {
    }
}