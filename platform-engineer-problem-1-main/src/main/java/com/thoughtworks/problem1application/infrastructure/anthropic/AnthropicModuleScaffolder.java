package com.thoughtworks.problem1application.infrastructure.anthropic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffolder;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldGenerationException;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;
import com.thoughtworks.problem1application.infrastructure.bedrock.ScaffoldPrompts;

import lombok.extern.slf4j.Slf4j;

/**
 * Mismo contrato que el adaptador de Bedrock, pero contra la API de Anthropic (Messages API).
 * Se fuerza la herramienta para que la salida llegue como JSON: {files: [{path, content}]}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "platform.scaffolder.provider", havingValue = "anthropic")
public class AnthropicModuleScaffolder implements ModuleScaffolder {

    private static final Map<String, Object> TOOL = Map.of(
            "name", ScaffoldPrompts.TOOL_NAME,
            "description", "Write the complete set of files for the root of the Terraform repository. "
                    + "Call it once with every file.",
            "input_schema", Map.of(
                    "type", "object",
                    "required", List.of("files"),
                    "properties", Map.of(
                            "files", Map.of(
                                    "type", "array",
                                    "items", Map.of(
                                            "type", "object",
                                            "required", List.of("path", "content"),
                                            "properties", Map.of(
                                                    "path", Map.of(
                                                            "type", "string",
                                                            "description", "Relative path, for example main.tf or backend/dev.backend.hcl"),
                                                    "content", Map.of(
                                                            "type", "string",
                                                            "description", "Complete file content, terraform fmt style, no markdown")))))));

    private final RestClient restClient;
    private final AnthropicProperties props;

    public AnthropicModuleScaffolder(AnthropicProperties props) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(props.timeout());
        this.restClient = RestClient.builder()
                .baseUrl(props.apiUrl())
                .requestFactory(requestFactory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.props = props;
    }

    @Override
    public Map<String, String> generate(ScaffoldRequest request, List<String> previousErrors) {
        Map<String, Object> body = Map.of(
                "model", props.modelId(),
                "max_tokens", props.maxTokens(),
                "temperature", 0,
                "system", ScaffoldPrompts.SYSTEM,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", ScaffoldPrompts.user(request, previousErrors))),
                "tools", List.of(TOOL),
                "tool_choice", Map.of("type", "tool", "name", ScaffoldPrompts.TOOL_NAME));

        MessageResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MessageResponse.class);
        } catch (RestClientException e) {
            throw new ScaffoldGenerationException("Anthropic API call failed: " + e.getMessage(), List.of());
        }
        if (response == null) {
            throw new ScaffoldGenerationException("Anthropic API returned an empty response", List.of());
        }

        log.info("Anthropic scaffold for {}: stopReason={}, inputTokens={}, outputTokens={}",
                request.repoName(), response.stopReason(),
                response.usage() == null ? null : response.usage().inputTokens(),
                response.usage() == null ? null : response.usage().outputTokens());

        if ("max_tokens".equals(response.stopReason())) {
            throw new ScaffoldGenerationException(
                    "The model output was truncated; raise platform.anthropic.max-tokens", List.of());
        }

        // Si no llamó la herramienta devolvemos vacío: el validador lo rechaza y se reintenta con feedback
        if (response.content() == null) {
            return Map.of();
        }
        return response.content().stream()
                .filter(block -> "tool_use".equals(block.type()) && block.input() != null)
                .findFirst()
                .map(block -> toFiles(block.input()))
                .orElseGet(Map::of);
    }

    private static Map<String, String> toFiles(Map<String, Object> input) {
        Map<String, String> files = new LinkedHashMap<>();
        if (!(input.get("files") instanceof List<?> items)) {
            return files;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> file
                    && file.get("path") instanceof String path
                    && file.get("content") instanceof String content) {
                files.put(path.strip(), content);
            }
        }
        return files;
    }

    private record MessageResponse(
            List<ContentBlock> content,
            @JsonProperty("stop_reason") String stopReason,
            Usage usage) {
    }

    private record ContentBlock(String type, Map<String, Object> input) {
    }

    private record Usage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens) {
    }
}