package com.thoughtworks.problem1application.unit.domain.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldValidator;
import com.thoughtworks.problem1application.helpers.ScaffoldFixtures;

class ScaffoldValidatorTest {

    private final ScaffoldValidator validator = new ScaffoldValidator();
    private final ScaffoldRequest request = ScaffoldFixtures.request();

    @Test
    void acceptsFilesThatFollowTheContract() {
        assertThat(validator.validate(request, ScaffoldFixtures.validFiles())).isEmpty();
    }

    @Test
    void reportsMissingAndUnexpectedFiles() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.remove("backend/qa.backend.hcl");
        files.put("extra.tf", "# nope");

        assertThat(validator.validate(request, files))
                .anyMatch(e -> e.equals("Missing file: backend/qa.backend.hcl"))
                .anyMatch(e -> e.startsWith("Unexpected file: extra.tf"));
    }

    @Test
    void rejectsResourcesAndModulesOutsideTheCatalog() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.merge("main.tf", """

                resource "aws_s3_bucket" "raw" {}

                module "other" {
                  source = "github.com/someone/terraform-aws-x"
                }
                """, String::concat);

        assertThat(validator.validate(request, files))
                .anyMatch(e -> e.contains("resource blocks are not allowed"))
                .anyMatch(e -> e.contains("module \"other\" was not requested"))
                .anyMatch(e -> e.contains("source \"github.com/someone/terraform-aws-x\" is not allowed"));
    }

    @Test
    void rejectsAParameterizedPlatformPolicy() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.computeIfPresent("main.tf", (path, content) ->
                content.replace("block_public_access = true", "block_public_access = var.public"));

        assertThat(validator.validate(request, files))
                .containsExactly("main.tf: module \"s3_bucket\" must set block_public_access = true (platform policy)");
    }

    @Test
    void requiresInputsToComeStraightFromTheVariableMap() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.computeIfPresent("main.tf", (path, content) ->
                content.replace("each.value.versioning_enabled", "try(each.value.versioning_enabled, false)"));

        assertThat(validator.validate(request, files))
                .containsExactly("main.tf: module \"s3_bucket\" must set versioning_enabled = each.value.versioning_enabled");
    }

    @Test
    void requiresAnEmptyPartialBackendAndTheRightStateKey() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.put("backend.tf", """
                terraform {
                  backend "s3" {
                    bucket = "somewhere"
                  }
                }
                """);
        files.computeIfPresent("backend/dev.backend.hcl", (path, content) ->
                content.replace("pagos-infra/dev.tfstate", "other/dev.tfstate"));

        assertThat(validator.validate(request, files)).containsExactlyInAnyOrder(
                "backend.tf: must declare an empty partial backend \"s3\" {}",
                "backend/dev.backend.hcl: key must be \"pagos-infra/dev.tfstate\"");
    }

    @Test
    void requiresTheModuleVariableAndEveryOutput() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.computeIfPresent("variables.tf", (path, content) -> content.replace("default = {}", ""));
        files.put("outputs.tf", """
                output "s3_bucket_bucket_id" {
                  value = { for k, m in module.s3_bucket : k => m.bucket_id }
                }
                """);

        assertThat(validator.validate(request, files)).containsExactlyInAnyOrder(
                "variables.tf: variable \"s3_bucket\" must have default = {}",
                "outputs.tf: missing output \"s3_bucket_bucket_arn\"");
    }

    @Test
    void rejectsMarkdownFences() {
        Map<String, String> files = ScaffoldFixtures.validFiles();
        files.computeIfPresent("provider.tf", (path, content) -> "```hcl\n" + content + "```\n");

        assertThat(validator.validate(request, files))
                .anyMatch(e -> e.equals("provider.tf: markdown fences are not allowed; return the raw file content"));
    }
}