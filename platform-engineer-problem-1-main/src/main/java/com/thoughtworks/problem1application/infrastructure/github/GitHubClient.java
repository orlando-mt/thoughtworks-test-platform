package com.thoughtworks.problem1application.infrastructure.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.problem1application.configurations.PlatformProperties;

@Component
public class GitHubClient {

    private static final MediaType RAW = MediaType.parseMediaType("application/vnd.github.raw+json");

    private final RestClient restClient;
    private final GitHubAppTokenProvider tokens;
    private final String org;

    public GitHubClient(PlatformProperties properties, GitHubAppTokenProvider tokens) {
        this.restClient = GitHubRestClients.create(properties.github().apiUrl());
        this.tokens = tokens;
        this.org = properties.github().org();
    }

    /** Contenido de un archivo tal cual (sin base64) en un repo de la org. */
    public String readRawFile(String repo, String path, String ref) {
        return restClient.get()
                .uri(builder -> builder.path("/repos/{org}/{repo}/contents/")
                        .path(path)
                        .queryParam("ref", ref)
                        .build(org, repo))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .accept(RAW)
                .retrieve()
                .body(String.class);
    }

    /** Crea un repo privado en la org copiando la estructura del repo plantilla. */
    public GitHubRepository createRepositoryFromTemplate(String templateRepo, String name, String description) {
        return restClient.post()
                .uri("/repos/{org}/{template}/generate", org, templateRepo)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "owner", org,
                        "name", name,
                        "description", description,
                        "private", true))
                .retrieve()
                .body(GitHubRepository.class);
    }

    public boolean repositoryExists(String name) {
        try {
            restClient.get()
                    .uri("/repos/{org}/{repo}", org, name)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /**
     * Sube varios archivos en UN solo commit sobre la rama (crea los que no existen, reemplaza los que sí).
     *
     * @param files ruta dentro del repo → contenido
     * @return sha del commit creado
     */
    public String commitFiles(String repo, String branch, String message, Map<String, String> files) {
        String parentSha = branchSha(repo, branch);
        String baseTreeSha = get(GitCommit.class, "/repos/{org}/{repo}/git/commits/{sha}", org, repo, parentSha)
                .tree().sha();

        List<Map<String, String>> entries = new ArrayList<>();
        files.forEach((path, content) -> entries.add(Map.of(
                "path", path,
                "mode", "100644",
                "type", "blob",
                "content", content)));

        String treeSha = send(HttpMethod.POST, Map.of("base_tree", baseTreeSha, "tree", entries),
                "/repos/{org}/{repo}/git/trees", org, repo).sha();

        String commitSha = send(HttpMethod.POST,
                Map.of("message", message, "tree", treeSha, "parents", List.of(parentSha)),
                "/repos/{org}/{repo}/git/commits", org, repo).sha();

        send(HttpMethod.PATCH, Map.of("sha", commitSha),
                "/repos/{org}/{repo}/git/refs/heads/{branch}", org, repo, branch);
        return commitSha;
    }

    /** Un repo recién generado desde la plantilla tarda unos segundos en tener su primer commit: se reintenta. */
    private String branchSha(String repo, String branch) {
        HttpClientErrorException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return get(GitRef.class, "/repos/{org}/{repo}/git/ref/heads/{branch}", org, repo, branch)
                        .object().sha();
            } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Conflict e) {
                last = e;
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    private <T> T get(Class<T> type, String uri, Object... variables) {
        return restClient.get()
                .uri(uri, variables)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(type);
    }

    private GitObject send(HttpMethod method, Object body, String uri, Object... variables) {
        return restClient.method(method)
                .uri(uri, variables)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GitObject.class);
    }

    private String bearer() {
        return "Bearer " + tokens.installationToken();
    }

    public record GitHubRepository(
            String name,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("default_branch") String defaultBranch) {
    }

    private record GitObject(String sha) {
    }

    private record GitRef(GitObject object) {
    }

    private record GitCommit(String sha, GitObject tree) {
    }
}