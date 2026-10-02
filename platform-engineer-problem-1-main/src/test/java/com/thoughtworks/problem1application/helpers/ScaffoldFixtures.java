package com.thoughtworks.problem1application.helpers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.problem1application.domain.scaffold.ScaffoldModule;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;

/** Un pedido de S3 y un juego de archivos que cumple el contrato. */
public final class ScaffoldFixtures {

    public static final String SOURCE = "github.com/orlando-mt/terraform-aws-s3?ref=v1.0.0";

    private ScaffoldFixtures() {
    }

    public static ScaffoldRequest request() {
        ScaffoldModule s3 = new ScaffoldModule(
                "s3-bucket", "s3_bucket", "S3 bucket", SOURCE, "bucket_name", "tags",
                List.of("versioning_enabled"), "{}",
                Map.of("block_public_access", "true", "force_destroy", "false"),
                List.of("bucket_id", "bucket_arn"));
        return new ScaffoldRequest("pagos", "pagos-infra", "us-east-1", "kordanix-platform-tfstate",
                ">= 5.0", List.of("dev", "qa", "prod"), List.of(s3));
    }

    public static Map<String, String> validFiles() {
        return validFiles("pagos-infra");
    }

    public static Map<String, String> validFiles(String repoName) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("provider.tf", """
                terraform {
                  required_version = ">= 1.10"
                  required_providers {
                    aws = {
                      source  = "hashicorp/aws"
                      version = ">= 5.0"
                    }
                  }
                }

                provider "aws" {
                  region = "us-east-1"
                }
                """);
        files.put("backend.tf", """
                terraform {
                  backend "s3" {}
                }
                """);
        files.put("variables.tf", """
                variable "environment" {
                  type = string
                }

                variable "s3_bucket" {
                  description = "Buckets keyed by name."
                  type = map(object({
                    versioning_enabled = optional(bool, false)
                  }))
                  default = {}
                }
                """);
        files.put("main.tf", """
                locals {
                  tags = { Environment = var.environment }
                }

                module "s3_bucket" {
                  source   = "github.com/orlando-mt/terraform-aws-s3?ref=v1.0.0"
                  for_each = var.s3_bucket

                  bucket_name         = each.key
                  versioning_enabled  = each.value.versioning_enabled
                  block_public_access = true
                  force_destroy       = false
                  tags                = local.tags
                }
                """);
        files.put("outputs.tf", """
                output "s3_bucket_bucket_id" {
                  value = { for k, m in module.s3_bucket : k => m.bucket_id }
                }

                output "s3_bucket_bucket_arn" {
                  value = { for k, m in module.s3_bucket : k => m.bucket_arn }
                }
                """);
        for (String env : List.of("dev", "qa", "prod")) {
            files.put("backend/" + env + ".backend.hcl", """
                    bucket       = "kordanix-platform-tfstate"
                    key          = "%s/%s.tfstate"
                    region       = "us-east-1"
                    encrypt      = true
                    use_lockfile = true
                    """.formatted(repoName, env));
        }
        return files;
    }
}