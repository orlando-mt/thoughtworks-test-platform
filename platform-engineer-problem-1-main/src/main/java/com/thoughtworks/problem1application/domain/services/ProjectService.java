package com.thoughtworks.problem1application.domain.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thoughtworks.problem1application.configurations.PlatformProperties;
import com.thoughtworks.problem1application.domain.catalog.ModuleCatalog;
import com.thoughtworks.problem1application.domain.entities.Project;
import com.thoughtworks.problem1application.domain.exceptions.InvalidResourceRequestException;
import com.thoughtworks.problem1application.domain.exceptions.ProjectAlreadyExistsException;
import com.thoughtworks.problem1application.domain.exceptions.ProjectNotFoundException;
import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffoldingService;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldResult;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient.GitHubRepository;
import com.thoughtworks.problem1application.infrastructure.repository.repositories.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final GitHubClient gitHub;
    private final PlatformProperties properties;
    private final ModuleCatalog catalog;
    private final ModuleScaffoldingService scaffolding;
    private final Pattern namePattern;

    public ProjectService(ProjectRepository projectRepository, GitHubClient gitHub, PlatformProperties properties,
                          ModuleCatalog catalog, ModuleScaffoldingService scaffolding) {
        this.projectRepository = projectRepository;
        this.gitHub = gitHub;
        this.properties = properties;
        this.catalog = catalog;
        this.scaffolding = scaffolding;
        this.namePattern = Pattern.compile(properties.namePattern());
    }

    @Transactional
    public Project create(String owner, String name) {
        if (name == null || !namePattern.matcher(name).matches()) {
            throw new InvalidResourceRequestException("Invalid project.",
                    List.of("name: must match " + properties.namePattern()));
        }
        if (projectRepository.existsByName(name)) {
            throw new ProjectAlreadyExistsException("Project '" + name + "' already exists.");
        }

        String repoName = name + properties.github().repoSuffix();
        // Defensa extra: el repo podría existir en GitHub aunque no en la base (por ejemplo, tras resetear la base local)
        if (gitHub.repositoryExists(repoName)) {
            throw new ProjectAlreadyExistsException(
                    "Repository '" + repoName + "' already exists in " + properties.github().org() + ".");
        }

        GitHubRepository repository = gitHub.createRepositoryFromTemplate(properties.github().templateRepo(),
                repoName, "Infrastructure of project " + name + ". Managed by the Kordanix platform.");

        Project project = new Project();
        project.setName(name);
        project.setRepoName(repository.name());
        project.setRepoUrl(repository.htmlUrl());
        project.setOwner(owner);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> listByOwner(String owner) {
        return projectRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional(readOnly = true)
    public Project getForOwner(String name, String owner) {
        // Si no es del usuario, 404 (no 403) para no revelar que existe
        return projectRepository.findByNameAndOwner(name, owner).orElseThrow(() -> new ProjectNotFoundException(name));
    }

    /**
     * Deja el repo listo para usar un módulo del catálogo: Bedrock escribe el Terraform,
     * el validador lo revisa y se sube en un commit directo a main.
     */
    public ModulePreparation prepareForModule(String owner, String projectName, String moduleId) {
        Project project = projectRepository.findByNameAndOwner(projectName, owner)
                .orElseThrow(() -> new ProjectNotFoundException(projectName));
        catalog.get(moduleId); // 404 si el módulo no está publicado

        if (project.getModules().contains(moduleId)) {
            return new ModulePreparation(project, moduleId, Map.of(), 0);
        }

        // Se regenera la raíz con TODOS los módulos del proyecto; los nombres fijos evitan que Terraform recree recursos
        Set<String> modules = new LinkedHashSet<>(project.getModules());
        modules.add(moduleId);
        ScaffoldResult result = scaffolding.scaffold(project.getName(), project.getRepoName(), modules);

        // [skip ci]: todavía no hay tfvars, no hay nada que aplicar
        gitHub.commitFiles(project.getRepoName(), "main",
                "chore(platform): add module " + moduleId + " [skip ci]", result.files());

        project.getModules().add(moduleId);
        Project saved = projectRepository.save(project);
        return new ModulePreparation(saved, moduleId, result.files(), result.attempts());
    }

    /** attempts = 0 significa que el módulo ya estaba y no se generó nada. */
    public record ModulePreparation(Project project, String moduleId, Map<String, String> files, int attempts) {
    }
}