package com.thoughtworks.problem1application.domain.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un recurso pedido por la plataforma. La base es la fuente de verdad; el tfvars se regenera desde aquí. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_RESOURCE")
public class ProvisionedResource {

    public enum Status { APPLYING, READY, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 64)
    private String moduleId;

    @Column(nullable = false, length = 16)
    private String environment;

    /** Nombre corto que eligió el usuario. */
    @Column(nullable = false, length = 32)
    private String name;

    /** Nombre real en AWS: <proyecto>-<ambiente>-<nombre>-<sufijo>. */
    @Column(nullable = false, unique = true, length = 100)
    private String physicalName;

    @Column(nullable = false, length = 4000)
    private String inputsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    private String commitSha;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}