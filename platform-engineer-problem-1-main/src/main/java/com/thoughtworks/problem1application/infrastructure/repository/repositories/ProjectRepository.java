package com.thoughtworks.problem1application.infrastructure.repository.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thoughtworks.problem1application.domain.entities.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByName(String name);

    Optional<Project> findByNameAndOwner(String name, String owner);

    List<Project> findByOwnerOrderByCreatedAtDesc(String owner);
}