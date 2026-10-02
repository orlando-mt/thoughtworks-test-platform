package com.thoughtworks.problem1application.domain.entities;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_PROJECT")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    /** Repo de infraestructura del proyecto en la org: <name>-infra */
    @Column(nullable = false, unique = true, length = 100)
    private String repoName;

    @Column(nullable = false)
    private String repoUrl;

    /** Email del usuario que lo creó (subject del JWT). */
    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private Instant createdAt;

    /** Ids del catálogo de los módulos que ya tienen su Terraform en el repo. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TB_PROJECT_MODULE", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "module_id", nullable = false)
    private Set<String> modules = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}