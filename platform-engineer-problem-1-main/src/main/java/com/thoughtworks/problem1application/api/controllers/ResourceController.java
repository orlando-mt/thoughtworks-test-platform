package com.thoughtworks.problem1application.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.request.CreateResourceRequestDTO;
import com.thoughtworks.problem1application.application.dtos.response.ResourceResponseDTO;
import com.thoughtworks.problem1application.domain.services.ResourceService;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects/{project}/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @Operation(summary = "Request a resource: validates the values, writes the tfvars and commits to main, which triggers the pipeline.")
    public ResponseEntity<ResourceResponseDTO> create(@RequestAttribute("claims") Claims claims,
                                                      @PathVariable String project,
                                                      @Valid @RequestBody CreateResourceRequestDTO request) {
        ResourceResponseDTO body = ResourceResponseDTO.from(resourceService.create(claims.getSubject(), project,
                request.moduleId(), request.environment(), request.name(), request.inputs()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping
    @Operation(summary = "List the resources of a project.")
    public List<ResourceResponseDTO> list(@RequestAttribute("claims") Claims claims, @PathVariable String project) {
        return resourceService.list(claims.getSubject(), project).stream().map(ResourceResponseDTO::from).toList();
    }
}