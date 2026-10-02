package com.thoughtworks.problem1application.functional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient;
import com.thoughtworks.problem1application.infrastructure.github.GitHubClient.GitHubRepository;

import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProjectFunctionalTest {

    @Autowired
    private MockMvc mock;

    @Autowired
    private JsonMapper mapper;

    @MockitoBean
    private GitHubClient gitHub;

    @BeforeEach
    void stubGitHub() {
        when(gitHub.createRepositoryFromTemplate(eq("infra-template"), anyString(), anyString()))
                .thenAnswer(call -> new GitHubRepository(call.getArgument(1),
                        "https://github.com/kordanix-io/" + call.getArgument(1), "main"));
    }

    @Test
    void shouldCreateProjectRepositoryFromTemplate() throws Exception {
        String name = uniqueName();

        mock.perform(post("/api/projects").header(AUTHORIZATION, login())
                        .contentType(APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/" + name))
                .andExpect(jsonPath("$.repoName").value(name + "-infra"))
                .andExpect(jsonPath("$.repoUrl").value("https://github.com/kordanix-io/" + name + "-infra"));

        verify(gitHub).createRepositoryFromTemplate(eq("infra-template"), eq(name + "-infra"), anyString());
    }

    @Test
    void shouldRejectInvalidName() throws Exception {
        mock.perform(post("/api/projects").header(AUTHORIZATION, login())
                        .contentType(APPLICATION_JSON).content(body("Pagos_App!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void shouldRejectDuplicateProject() throws Exception {
        String token = login();
        String name = uniqueName();
        mock.perform(post("/api/projects").header(AUTHORIZATION, token).contentType(APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated());
        mock.perform(post("/api/projects").header(AUTHORIZATION, token).contentType(APPLICATION_JSON).content(body(name)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldNotOverwriteRepositoryThatAlreadyExistsInGitHub() throws Exception {
        String name = uniqueName();
        when(gitHub.repositoryExists(name + "-infra")).thenReturn(true);

        mock.perform(post("/api/projects").header(AUTHORIZATION, login())
                        .contentType(APPLICATION_JSON).content(body(name)))
                .andExpect(status().isConflict());

        verify(gitHub, never()).createRepositoryFromTemplate(anyString(), anyString(), anyString());
    }

    @Test
    void shouldHideProjectsFromOtherUsers() throws Exception {
        String owner = login();
        String name = uniqueName();
        mock.perform(post("/api/projects").header(AUTHORIZATION, owner).contentType(APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated());

        String stranger = login();
        mock.perform(get("/api/projects/" + name).header(AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
        mock.perform(get("/api/projects").header(AUTHORIZATION, stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mock.perform(get("/api/projects/" + name).header(AUTHORIZATION, owner))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireToken() throws Exception {
        mock.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    private String body(String name) {
        return mapper.writeValueAsString(Map.of("name", name));
    }

    /** Cumple el patrón de nombres: empieza con letra y solo minúsculas, números y guiones. */
    private static String uniqueName() {
        return "p" + UUID.randomUUID().toString().substring(0, 8);
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
}