package com.thoughtworks.problem1application.infrastructure.bedrock;

import java.util.List;
import java.util.stream.Collectors;

import com.thoughtworks.problem1application.domain.scaffold.ScaffoldModule;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;

/** El contrato en lenguaje natural. Cada regla tiene su chequeo en ScaffoldValidator. */
public final class ScaffoldPrompts {

    public static final String TOOL_NAME = "write_terraform_files";

    public static final String SYSTEM = """
            You write the root configuration of a Terraform "live" repository for Kordanix's internal developer platform.
            The repository only consumes published, versioned modules from the platform catalog. It never creates resources directly.

            Return the files ONLY by calling the tool write_terraform_files once, with every file.
            File contents are raw HCL in terraform fmt style (2-space indentation, aligned "="). No markdown fences.

            Contract. The platform validates every rule and rejects the whole output if one fails:
            1. Files: exactly the paths listed in the request, nothing else.
            2. provider.tf: a terraform block with required_version = ">= 1.10" and required_providers
               aws = { source = "hashicorp/aws", version = <the constraint given> }; then provider "aws" with the exact
               region given and default_tags { tags = { Project = <project>, Environment = var.environment, ManagedBy = "terraform" } }.
            3. backend.tf: terraform { backend "s3" {} } with nothing inside the braces (partial configuration).
            4. backend/<env>.backend.hcl, one per environment: bucket, key and region exactly as given, plus encrypt = true
               and use_lockfile = true.
            5. variables.tf:
               - variable "environment" of type string, with a validation block that only accepts the given environments.
               - one variable per module, named exactly as the module label, with type map(object({ ... })) and default = {}.
                 Map keys are resource names. The object has one attribute per module input listed in the request, and only
                 those, typed from the inputs JSON Schema. Use optional(<type>, <default>) when the schema gives a default or the
                 input is not required.
            6. main.tf:
               - a locals block with tags = { Project = <project>, Environment = var.environment, ManagedBy = "terraform", Platform = "kordanix" }.
               - one module block per module, labelled exactly as the module label, with: source = the exact source string given;
                 for_each = var.<label>; <name variable> = each.key; every input as <input> = each.value.<input> (no try or lookup,
                 defaults belong in variables.tf); every fixed value exactly as given (they are platform policies, never turn
                 them into variables); <tags variable> = local.tags when the module has a tags variable.
               - no resource, data, provisioner or other module blocks.
            7. outputs.tf: for every module output, output "<label>_<output>" with a description and
               value = { for k, m in module.<label> : k => m.<output> }. If no module has outputs, write one comment line.
            8. Use only what the request gives you. Never invent modules, inputs, outputs or values.

            Example for an unrelated module (label sqs_queue, name variable queue_name, input fifo, fixed sse_enabled = true,
            tags variable tags, output queue_arn):

            variables.tf
            variable "sqs_queue" {
              description = "SQS queues to create, keyed by queue name."
              type = map(object({
                fifo = optional(bool, false)
              }))
              default = {}
            }

            main.tf
            module "sqs_queue" {
              source   = "github.com/example-org/terraform-aws-sqs?ref=v2.1.0"
              for_each = var.sqs_queue

              queue_name  = each.key
              fifo        = each.value.fifo
              sse_enabled = true
              tags        = local.tags
            }

            outputs.tf
            output "sqs_queue_queue_arn" {
              description = "ARN of each SQS queue, keyed by queue name."
              value       = { for k, m in module.sqs_queue : k => m.queue_arn }
            }
            """;

    private ScaffoldPrompts() {
    }

    public static String user(ScaffoldRequest request, List<String> previousErrors) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project: ").append(request.project()).append('\n')
                .append("Repository: ").append(request.repoName()).append('\n')
                .append("AWS region: ").append(request.region()).append('\n')
                .append("AWS provider version constraint: ").append(request.awsProviderVersion()).append('\n')
                .append("Environments: ").append(String.join(", ", request.environments())).append("\n\n");

        sb.append("Files to write:\n");
        request.expectedPaths().forEach(path -> sb.append("- ").append(path).append('\n'));

        sb.append("\nBackend configuration per environment:\n");
        for (String env : request.environments()) {
            sb.append("- ").append(ScaffoldRequest.backendConfigPath(env))
                    .append(": bucket = \"").append(request.stateBucket())
                    .append("\", key = \"").append(request.stateKey(env))
                    .append("\", region = \"").append(request.region()).append("\"\n");
        }

        sb.append("\nModules:\n");
        for (ScaffoldModule module : request.modules()) {
            sb.append("\n## ").append(module.label()).append(" (catalog id ").append(module.id()).append(")\n")
                    .append("Description: ").append(module.description()).append('\n')
                    .append("Source: ").append(module.source()).append('\n')
                    .append("Name variable: ").append(module.nameVariable()).append('\n')
                    .append("Tags variable: ").append(module.tagsVariable() == null ? "(none)" : module.tagsVariable()).append('\n')
                    .append("Inputs: ").append(module.inputs().isEmpty() ? "(none)" : String.join(", ", module.inputs())).append('\n')
                    .append("Inputs JSON Schema (properties not listed as inputs are set by the platform): ")
                    .append(module.inputsSchema()).append('\n')
                    .append("Fixed values: ").append(module.fixed().isEmpty() ? "(none)" : module.fixed().entrySet().stream()
                            .map(e -> e.getKey() + " = " + e.getValue())
                            .collect(Collectors.joining(", "))).append('\n')
                    .append("Outputs: ").append(module.outputs().isEmpty() ? "(none)" : String.join(", ", module.outputs())).append('\n');
        }

        if (!previousErrors.isEmpty()) {
            sb.append("\nYour previous attempt was rejected by the platform validator. Fix every point and return all files again:\n");
            previousErrors.forEach(error -> sb.append("- ").append(error).append('\n'));
        }
        return sb.toString();
    }
}