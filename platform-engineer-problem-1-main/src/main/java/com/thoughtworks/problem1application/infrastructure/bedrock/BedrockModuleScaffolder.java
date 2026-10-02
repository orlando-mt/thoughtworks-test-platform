package com.thoughtworks.problem1application.infrastructure.bedrock;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffolder;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldGenerationException;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SpecificToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

/**
 * Pide los archivos a Bedrock (Converse API) forzando una herramienta: la salida llega como JSON
 * estructurado ({files: [{path, content}]}) y no como texto que haya que parsear.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "platform.scaffolder.provider", havingValue = "bedrock", matchIfMissing = true)
public class BedrockModuleScaffolder implements ModuleScaffolder {

    private static final ToolConfiguration TOOL_CONFIG = toolConfig();

    private final BedrockRuntimeClient client;
    private final BedrockProperties props;

    public BedrockModuleScaffolder(BedrockRuntimeClient client, BedrockProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Map<String, String> generate(ScaffoldRequest request, List<String> previousErrors) {
        ConverseRequest converse = ConverseRequest.builder()
                .modelId(props.modelId())
                .system(SystemContentBlock.builder().text(ScaffoldPrompts.SYSTEM).build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.builder().text(ScaffoldPrompts.user(request, previousErrors)).build())
                        .build())
                .toolConfig(TOOL_CONFIG)
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(props.maxTokens())
                        .temperature(0f)
                        .build())
                .build();

        ConverseResponse response;
        try {
            response = client.converse(converse);
        } catch (SdkException e) {
            throw new ScaffoldGenerationException("Bedrock call failed: " + e.getMessage(), List.of());
        }

        log.info("Bedrock scaffold for {}: stopReason={}, inputTokens={}, outputTokens={}",
                request.repoName(), response.stopReason(),
                response.usage().inputTokens(), response.usage().outputTokens());

        if (response.stopReason() == StopReason.MAX_TOKENS) {
            throw new ScaffoldGenerationException("The model output was truncated; raise platform.bedrock.max-tokens", List.of());
        }

        ToolUseBlock toolUse = response.output().message().content().stream()
                .map(ContentBlock::toolUse)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        // Si no llamó la herramienta devolvemos vacío: el validador lo rechaza y se reintenta con feedback
        return toolUse == null ? Map.of() : toFiles(toolUse.input());
    }

    private static Map<String, String> toFiles(Document input) {
        Map<String, String> files = new LinkedHashMap<>();
        if (input == null || !input.isMap()) {
            return files;
        }
        Document list = input.asMap().get("files");
        if (list == null || !list.isList()) {
            return files;
        }
        for (Document item : list.asList()) {
            if (!item.isMap()) {
                continue;
            }
            Document path = item.asMap().get("path");
            Document content = item.asMap().get("content");
            if (path != null && path.isString() && content != null && content.isString()) {
                files.put(path.asString().strip(), content.asString());
            }
        }
        return files;
    }

    private static ToolConfiguration toolConfig() {
        Document file = object(Map.of(
                        "path", string("Relative path, for example main.tf or backend/dev.backend.hcl"),
                        "content", string("Complete file content, terraform fmt style, no markdown")),
                "path", "content");
        Document schema = object(Map.of(
                        "files", Document.fromMap(Map.of(
                                "type", Document.fromString("array"),
                                "items", file))),
                "files");

        ToolSpecification spec = ToolSpecification.builder()
                .name(ScaffoldPrompts.TOOL_NAME)
                .description("Write the complete set of files for the root of the Terraform repository. Call it once with every file.")
                .inputSchema(ToolInputSchema.builder().json(schema).build())
                .build();

        return ToolConfiguration.builder()
                .tools(Tool.builder().toolSpec(spec).build())
                .toolChoice(ToolChoice.builder()
                        .tool(SpecificToolChoice.builder().name(ScaffoldPrompts.TOOL_NAME).build())
                        .build())
                .build();
    }

    private static Document object(Map<String, Document> properties, String... required) {
        return Document.fromMap(Map.of(
                "type", Document.fromString("object"),
                "properties", Document.fromMap(properties),
                "required", Document.fromList(Arrays.stream(required).map(Document::fromString).toList())));
    }

    private static Document string(String description) {
        return Document.fromMap(Map.of(
                "type", Document.fromString("string"),
                "description", Document.fromString(description)));
    }
}