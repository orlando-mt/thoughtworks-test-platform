package com.thoughtworks.problem1application.functional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thoughtworks.problem1application.application.dtos.response.AuthenticateResponseDTO;
import com.thoughtworks.problem1application.domain.catalog.CatalogSource;
import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffolder;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;
import com.thoughtworks.problem1application.helpers.ScaffoldFixtures;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient.GitHubRepository;

import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ResourceFunctionalTest {

    @Autowired
    private MockMvc mock;

    @Autowired
    private JsonMapper mapper;

    @MockitoBean
    private GitHubClient gitHub;

    @MockitoBean
    private CatalogSource catalogSource;

    @MockitoBean
    private ModuleScaffolder scaffolder;

    @BeforeEach
    void stubs() throws IOException {
        when(catalogSource.read("catalog.yml")).thenReturn(resource("catalog/catalog.yml"));
        when(catalogSource.read("generated/s3-bucket.json")).thenReturn(resource("catalog/generated/s3-bucket.json"));
        when(gitHub.createRepositoryFromTemplate(eq("infra-template"), anyString(), anyString()))
                .thenAnswer(call -> new GitHubRepository(call.getArgument(1),
                        "https://github.com/kordanix-io/" + call.getArgument(1), "main"));
        when(scaffolder.generate(any(), anyList()))
                .thenAnswer(call -> ScaffoldFixtures.validFiles(((ScaffoldRequest) call.getArgument(0)).repoName()));
    }

    @Test
    void shouldCommitTheTfvarsOfTheEnvironmentToMain() throws Exception {
        String token = login();
        String project = preparedProject(token);

        mock.perform(post("/api/projects/" + project + "/resources").header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content(body("dev", "reportes", Map.of("versioning_enabled", false))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("APPLYING"))
                .andExpect(jsonPath("$.inputs.versioning_enabled").value(false))
                .andExpect(jsonPath("$.pipelineUrl").value("https://github.com/kordanix-io/" + project + "-infra/actions"));

        verify(gitHub).commitFiles(eq(project + "-infra"), eq("main"), contains("feat(dev)"),
                argThat(files -> files.size() == 1
                        && files.get("environments/dev.tfvars").contains("environment = \"dev\"")
                        && files.get("environments/dev.tfvars").contains("\"" + project + "-dev-reportes-")
                        && files.get("environments/dev.tfvars").contains("versioning_enabled = false")));
        mock.perform(get("/api/projects/" + project + "/resources").header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldUseTheModuleDefaultWhenAnInputIsNotSent() throws Exception {
        String token = login();
        String project = preparedProject(token);

        mock.perform(post("/api/projects/" + project + "/resources").header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(body("dev", "reportes", Map.of())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inputs.versioning_enabled").value(true));
    }

    @Test
    void shouldRejectEnvironmentsThatAreNotSelfService() throws Exception {
        String token = login();
        String project = preparedProject(token);

        mock.perform(post("/api/projects/" + project + "/resources").header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(body("prod", "reportes", Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.policy").value("self-service-environments"));
    }

    @Test
    void shouldRejectUnknownInputsAndWrongTypes() throws Exception {
        String token = login();
        String project = preparedProject(token);

        mock.perform(post("/api/projects/" + project + "/resources").header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content(body("dev", "reportes", Map.of("versioning_enabled", "yes", "force_destroy", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verify(gitHub, never()).commitFiles(anyString(), anyString(), contains("feat("), any());
    }

    private String body(String environment, String name, Map<String, Object> inputs) {
        return mapper.writeValueAsString(Map.of(
                "moduleId", "s3-bucket", "environment", environment, "name", name, "inputs", inputs));
    }

    private String preparedProject(String token) throws Exception {
        String name = "p" + UUID.randomUUID().toString().substring(0, 8);
        mock.perform(post("/api/projects").header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated());
        mock.perform(post("/api/projects/" + name + "/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isCreated());
        return name;
    }

    private String login() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mock.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Test User", "email", email, "password", "q1w2e3"))))
                .andExpect(status().isCreated());
        String response = mock.perform(post("/api/auth").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "q1w2e3"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readValue(response, AuthenticateResponseDTO.class).getAccessToken();
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ResourceFunctionalTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}