package com.thoughtworks.problem1application.domain.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.thoughtworks.problem1application.configurations.PlatformProperties;
import com.thoughtworks.problem1application.domain.catalog.InputType;
import com.thoughtworks.problem1application.domain.catalog.ModuleCatalog;
import com.thoughtworks.problem1application.domain.catalog.ModuleDefinition;
import com.thoughtworks.problem1application.domain.catalog.ModuleInput;
import com.thoughtworks.problem1application.domain.entities.Project;
import com.thoughtworks.problem1application.domain.entities.ProvisionedResource;
import com.thoughtworks.problem1application.domain.exceptions.InvalidResourceRequestException;
import com.thoughtworks.problem1application.domain.exceptions.PolicyViolationException;
import com.thoughtworks.problem1application.domain.exceptions.ProjectNotFoundException;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldModule;
import com.thoughtworks.problem1application.domain.scaffold.TfvarsRenderer;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient;
import com.thoughtworks.problem1application.infrastructure.repository.repositories.ProjectRepository;
import com.thoughtworks.problem1application.infrastructure.repository.repositories.ProvisionedResourceRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ResourceService {

    private static final TypeReference<LinkedHashMap<String, Object>> INPUTS = new TypeReference<>() {
    };

    private final ProjectRepository projectRepository;
    private final ProvisionedResourceRepository resourceRepository;
    private final ModuleCatalog catalog;
    private final GitHubClient gitHub;
    private final PlatformProperties properties;
    private final JsonMapper jsonMapper;
    private final Pattern namePattern;

    public ResourceService(ProjectRepository projectRepository, ProvisionedResourceRepository resourceRepository,
                           ModuleCatalog catalog, GitHubClient gitHub, PlatformProperties properties,
                           JsonMapper jsonMapper) {
        this.projectRepository = projectRepository;
        this.resourceRepository = resourceRepository;
        this.catalog = catalog;
        this.gitHub = gitHub;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.namePattern = Pattern.compile(properties.namePattern());
    }

    /** Valida el pedido, regenera el tfvars del ambiente y lo sube a main: ese commit dispara el pipeline. */
    public ResourceView create(String owner, String projectName, String moduleId, String environment,
                               String name, Map<String, Object> requestedInputs) {
        Project project = findProject(owner, projectName);
        ModuleDefinition module = catalog.get(moduleId);
        Map<String, Object> requested = requestedInputs == null ? Map.of() : requestedInputs;

        List<String> errors = new ArrayList<>();
        if (!project.getModules().contains(moduleId)) {
            errors.add("moduleId: the project is not prepared for '" + moduleId
                    + "'. Call POST /api/projects/" + projectName + "/modules/" + moduleId + " first.");
        }
        if (!properties.environments().contains(environment)) {
            errors.add("environment: must be one of " + properties.environments());
        }
        if (name == null || !namePattern.matcher(name).matches()) {
            errors.add("name: must match " + properties.namePattern());
        }
        Map<String, Object> inputs = resolveInputs(module, requested, errors);
        if (errors.isEmpty() && resourceRepository.existsByProjectIdAndEnvironmentAndModuleIdAndName(
                project.getId(), environment, moduleId, name)) {
            errors.add("name: '" + name + "' already exists for " + moduleId + " in " + environment);
        }
        if (!errors.isEmpty()) {
            throw new InvalidResourceRequestException(errors);
        }
        if (!properties.selfServiceEnvironments().contains(environment)) {
            throw new PolicyViolationException("self-service-environments",
                    "Environment '" + environment + "' is not self-service. Allowed: "
                            + properties.selfServiceEnvironments() + ". Ask the platform team.");
        }

        ProvisionedResource resource = new ProvisionedResource();
        resource.setProjectId(project.getId());
        resource.setModuleId(moduleId);
        resource.setEnvironment(environment);
        resource.setName(name);
        resource.setPhysicalName(project.getName() + "-" + environment + "-" + name + "-"
                + UUID.randomUUID().toString().substring(0, 6));
        resource.setInputsJson(jsonMapper.writeValueAsString(inputs));
        resource.setStatus(ProvisionedResource.Status.APPLYING);
        resource.setRequestedBy(owner);

        List<ProvisionedResource> inEnvironment = new ArrayList<>(
                resourceRepository.findByProjectIdAndEnvironmentOrderByCreatedAtAsc(project.getId(), environment));
        inEnvironment.add(resource);

        // Primero el commit: si GitHub falla, no queda un recurso fantasma en la base
        String commitSha = gitHub.commitFiles(project.getRepoName(), "main",
                "feat(" + environment + "): add " + moduleId + " " + resource.getPhysicalName(),
                Map.of("environments/" + environment + ".tfvars", renderTfvars(project, environment, inEnvironment)));

        resource.setCommitSha(commitSha);
        return view(project, resourceRepository.save(resource));
    }

    public List<ResourceView> list(String owner, String projectName) {
        Project project = findProject(owner, projectName);
        return resourceRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(resource -> view(project, resource))
                .toList();
    }

    private Project findProject(String owner, String projectName) {
        return projectRepository.findByNameAndOwner(projectName, owner)
                .orElseThrow(() -> new ProjectNotFoundException(projectName));
    }

    /** Solo los inputs preguntables del catálogo; lo que el usuario no manda toma el default del módulo. */
    private Map<String, Object> resolveInputs(ModuleDefinition module, Map<String, Object> requested,
                                              List<String> errors) {
        Set<String> platformOwned = new HashSet<>();
        platformOwned.add(module.nameVariable());
        if (module.tagsVariable() != null) {
            platformOwned.add(module.tagsVariable());
        }
        if (module.enforced() != null) {
            platformOwned.addAll(module.enforced().keySet());
        }
        List<ModuleInput> askable = module.inputs() == null ? List.of() : module.inputs().stream()
                .filter(input -> !platformOwned.contains(input.name()))
                .toList();
        Set<String> known = new HashSet<>();
        askable.forEach(input -> known.add(input.name()));

        requested.keySet().stream()
                .filter(key -> !known.contains(key))
                .forEach(key -> errors.add("inputs." + key + ": is not a configurable input of " + module.id()));

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (ModuleInput input : askable) {
            Object value = requested.containsKey(input.name()) ? requested.get(input.name()) : input.defaultValue();
            if (value == null) {
                if (input.required()) {
                    errors.add("inputs." + input.name() + ": is required");
                }
                continue;
            }
            if (!matches(input.type(), value)) {
                errors.add("inputs." + input.name() + ": must be a " + input.type().jsonType());
                continue;
            }
            resolved.put(input.name(), value);
        }
        return resolved;
    }

    private static boolean matches(InputType type, Object value) {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
        };
    }

    private String renderTfvars(Project project, String environment, List<ProvisionedResource> resources) {
        Map<String, Map<String, Map<String, Object>>> byVariable = new LinkedHashMap<>();
        for (String moduleId : project.getModules()) {
            Map<String, Map<String, Object>> ofModule = new LinkedHashMap<>();
            resources.stream()
                    .filter(resource -> resource.getModuleId().equals(moduleId))
                    .forEach(resource -> ofModule.put(resource.getPhysicalName(), readInputs(resource)));
            if (!ofModule.isEmpty()) {
                byVariable.put(ScaffoldModule.labelOf(moduleId), ofModule);
            }
        }
        return TfvarsRenderer.render(environment, byVariable);
    }

    private Map<String, Object> readInputs(ProvisionedResource resource) {
        return jsonMapper.readValue(resource.getInputsJson(), INPUTS);
    }

    private ResourceView view(Project project, ProvisionedResource resource) {
        return new ResourceView(resource, readInputs(resource), project.getRepoUrl() + "/actions");
    }

    public record ResourceView(ProvisionedResource resource, Map<String, Object> inputs, String pipelineUrl) {
    }
}