package com.thoughtworks.problem1application.functional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
class ProjectModuleFunctionalTest {

    @Autowired
    private MockMvc mock;

    @Autowired
    private JsonMapper mapper;

    @MockitoBean
    private GitHubClient gitHub;

    @MockitoBean
    private CatalogSource catalogSource;

    /** Reemplaza a Bedrock: devuelve archivos válidos para el repo que le pidan. */
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
    void shouldGenerateTheModuleFilesAndCommitThemToMain() throws Exception {
        String token = login();
        String name = createProject(token);

        mock.perform(post("/api/projects/" + name + "/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generated").value(true))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.modules[0]").value("s3-bucket"))
                .andExpect(jsonPath("$.files['main.tf']").exists());

        verify(gitHub).commitFiles(eq(name + "-infra"), eq("main"), contains("s3-bucket"), anyMap());
        mock.perform(get("/api/projects/" + name).header(AUTHORIZATION, token))
                .andExpect(jsonPath("$.modules[0]").value("s3-bucket"));
    }

    @Test
    void shouldNotGenerateAgainAModuleTheProjectAlreadyHas() throws Exception {
        String token = login();
        String name = createProject(token);
        mock.perform(post("/api/projects/" + name + "/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isCreated());

        mock.perform(post("/api/projects/" + name + "/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated").value(false));

        verify(gitHub, times(1)).commitFiles(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldNotCommitWhenTheModelNeverProducesValidTerraform() throws Exception {
        String token = login();
        String name = createProject(token);
        doReturn(Map.of()).when(scaffolder).generate(any(), anyList());

        mock.perform(post("/api/projects/" + name + "/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errors").isArray());

        verify(gitHub, never()).commitFiles(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldReturn404ForUnknownModuleOrProject() throws Exception {
        String token = login();
        String name = createProject(token);

        mock.perform(post("/api/projects/" + name + "/modules/quantum-computer").header(AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        mock.perform(post("/api/projects/nope/modules/s3-bucket").header(AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    private String createProject(String token) throws Exception {
        String name = "p" + UUID.randomUUID().toString().substring(0, 8);
        mock.perform(post("/api/projects").header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", name))))
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
        try (InputStream in = ProjectModuleFunctionalTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}