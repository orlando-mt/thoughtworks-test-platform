package com.thoughtworks.problem1application.api.controllers;

import java.net.URI;
import java.util.List;

import com.thoughtworks.problem1application.application.dtos.response.PrepareProjectResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.request.CreateProjectRequestDTO;
import com.thoughtworks.problem1application.application.dtos.response.ProjectResponseDTO;
import com.thoughtworks.problem1application.domain.entities.Project;
import com.thoughtworks.problem1application.domain.services.ProjectService;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a project: a private <name>-infra repository generated from the platform template.")
    public ResponseEntity<ProjectResponseDTO> create(@RequestAttribute("claims") Claims claims,
                                                     @Valid @RequestBody CreateProjectRequestDTO request) {
        Project project = projectService.create(claims.getSubject(), request.name());
        return ResponseEntity.created(URI.create("/api/projects/" + project.getName()))
                .body(ProjectResponseDTO.from(project));
    }

    @GetMapping
    @Operation(summary = "List the projects owned by the authenticated user.")
    public List<ProjectResponseDTO> list(@RequestAttribute("claims") Claims claims) {
        return projectService.listByOwner(claims.getSubject()).stream().map(ProjectResponseDTO::from).toList();
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a project.")
    public ProjectResponseDTO get(@RequestAttribute("claims") Claims claims, @PathVariable String name) {
        return ProjectResponseDTO.from(projectService.getForOwner(name, claims.getSubject()));
    }

    @PostMapping("/{name}/modules/{moduleId}")
    @Operation(summary = "Add a catalog module to the project: Bedrock writes the Terraform files and they are committed to the repository.")
    public ResponseEntity<PrepareProjectResponseDTO> addModule(@RequestAttribute("claims") Claims claims,
                                                               @PathVariable String name,
                                                               @PathVariable String moduleId) {
        PrepareProjectResponseDTO body = PrepareProjectResponseDTO.from(
                projectService.prepareForModule(claims.getSubject(), name, moduleId));
        return ResponseEntity.status(body.generated() ? HttpStatus.CREATED : HttpStatus.OK).body(body);
    }
}