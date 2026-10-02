package com.thoughtworks.problem1application.domain.scaffold;

import java.util.ArrayList;
import java.util.List;

/** Todo lo que el backend fija; el modelo solo escribe el HCL. */
public record ScaffoldRequest(
        String project,
        String repoName,
        String region,
        String stateBucket,
        String awsProviderVersion,
        List<String> environments,
        List<ScaffoldModule> modules) {

    public static final List<String> ROOT_FILES =
            List.of("provider.tf", "backend.tf", "variables.tf", "main.tf", "outputs.tf");

    public List<String> expectedPaths() {
        List<String> paths = new ArrayList<>(ROOT_FILES);
        environments.forEach(env -> paths.add(backendConfigPath(env)));
        return List.copyOf(paths);
    }

    public static String backendConfigPath(String env) {
        return "backend/" + env + ".backend.hcl";
    }

    public String stateKey(String env) {
        return repoName + "/" + env + ".tfstate";
    }
}