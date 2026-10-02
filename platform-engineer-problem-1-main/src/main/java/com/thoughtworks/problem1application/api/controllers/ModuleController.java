package com.thoughtworks.problem1application.api.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.response.ModuleDetailDTO;
import com.thoughtworks.problem1application.application.dtos.response.ModuleSummaryDTO;
import com.thoughtworks.problem1application.domain.catalog.ModuleCatalog;
import com.thoughtworks.problem1application.domain.catalog.ModuleDefinition;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleCatalog catalog;

    public ModuleController(ModuleCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "List the Terraform modules published in the platform catalog.")
    public List<ModuleSummaryDTO> list() {
        return catalog.all().stream()
                .map(m -> new ModuleSummaryDTO(m.id(), m.displayName(), m.description(), m.version()))
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a module: what can be asked (JSON Schema), what the platform enforces, and its outputs.")
    public ModuleDetailDTO get(@PathVariable String id) {
        ModuleDefinition module = catalog.get(id);
        return new ModuleDetailDTO(module.id(), module.displayName(), module.description(), module.source(),
                module.version(), catalog.schemaFor(module), module.enforced(), module.outputs());
    }
}