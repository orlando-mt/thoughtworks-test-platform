package com.thoughtworks.problem1application.infrastructure.repository.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thoughtworks.problem1application.domain.entities.ProvisionedResource;

@Repository
public interface ProvisionedResourceRepository extends JpaRepository<ProvisionedResource, UUID> {

    List<ProvisionedResource> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    List<ProvisionedResource> findByProjectIdAndEnvironmentOrderByCreatedAtAsc(UUID projectId, String environment);

    boolean existsByProjectIdAndEnvironmentAndModuleIdAndName(UUID projectId, String environment,
                                                              String moduleId, String name);
}