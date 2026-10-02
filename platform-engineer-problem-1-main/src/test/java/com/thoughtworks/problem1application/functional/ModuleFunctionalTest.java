package com.thoughtworks.problem1application.functional;

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

import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ModuleFunctionalTest {

    @Autowired
    private MockMvc mock;

    @Autowired
    private JsonMapper mapper;

    @MockitoBean
    private CatalogSource catalogSource;

    @BeforeEach
    void stubCatalog() throws IOException {
        when(catalogSource.read("catalog.yml")).thenReturn(resource("catalog/catalog.yml"));
        when(catalogSource.read("generated/s3-bucket.json")).thenReturn(resource("catalog/generated/s3-bucket.json"));
    }

    @Test
    void shouldListPublishedModules() throws Exception {
        mock.perform(get("/api/modules").header(AUTHORIZATION, login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("s3-bucket"))
                .andExpect(jsonPath("$[0].version").value("v1.0.0"));
    }

    @Test
    void shouldReturnSchemaEnforcedValuesAndOutputs() throws Exception {
        mock.perform(get("/api/modules/s3-bucket").header(AUTHORIZATION, login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema.properties.inputs.properties.versioning_enabled.type").value("boolean"))
                .andExpect(jsonPath("$.schema.properties.inputs.properties.versioning_enabled.default").value(true))
                .andExpect(jsonPath("$.schema.properties.inputs.additionalProperties").value(false))
                .andExpect(jsonPath("$.enforced.block_public_access").value(true))
                .andExpect(jsonPath("$.outputs.length()").value(2));
    }

    @Test
    void shouldReturn404ForUnpublishedModule() throws Exception {
        mock.perform(get("/api/modules/quantum-computer").header(AUTHORIZATION, login()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireToken() throws Exception {
        mock.perform(get("/api/modules")).andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mock.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Test User", "email", email, "password", "q1w2e3"))))
                .andExpect(status().isCreated());
        String body = mock.perform(post("/api/auth").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "q1w2e3"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readValue(body, AuthenticateResponseDTO.class).getAccessToken();
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ModuleFunctionalTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}