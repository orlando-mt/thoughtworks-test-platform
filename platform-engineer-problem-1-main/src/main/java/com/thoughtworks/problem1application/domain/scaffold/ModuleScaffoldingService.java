package com.thoughtworks.problem1application.domain.scaffold;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Generar → validar → devolver los errores al modelo, hasta maxAttempts. */
@Slf4j
@Service
public class ModuleScaffoldingService {

    private final ScaffoldRequestFactory requestFactory;
    private final ModuleScaffolder scaffolder;
    private final ScaffoldValidator validator;
    private final int maxAttempts;

    public ModuleScaffoldingService(ScaffoldRequestFactory requestFactory,
                                    ModuleScaffolder scaffolder,
                                    ScaffoldValidator validator,
                                    @Value("${platform.bedrock.max-attempts:3}") int maxAttempts) {
        this.requestFactory = requestFactory;
        this.scaffolder = scaffolder;
        this.validator = validator;
        this.maxAttempts = maxAttempts;
    }

    public ScaffoldResult scaffold(String project, String repoName, Collection<String> moduleIds) {
        ScaffoldRequest request = requestFactory.build(project, repoName, moduleIds);
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, String> files = scaffolder.generate(request, errors);
            errors = validator.validate(request, files);
            if (errors.isEmpty()) {
                log.info("Scaffold for {} accepted on attempt {}", repoName, attempt);
                return new ScaffoldResult(files, attempt);
            }
            log.warn("Scaffold for {} rejected on attempt {}/{}: {}", repoName, attempt, maxAttempts, errors);
        }
        throw new ScaffoldGenerationException(
                "The model could not produce valid Terraform after " + maxAttempts + " attempts", errors);
    }
}