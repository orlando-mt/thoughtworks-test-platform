variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "name_prefix" {
  description = "Same prefix used by the infra repository"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "state_bucket" {
  description = "Bucket holding the Terraform states, including the one of the infra repository"
  type        = string
}

variable "image_tag" {
  description = "Tag of the api and web images. The pipeline passes the commit SHA"
  type        = string
}

variable "github_org" {
  description = "GitHub organization where the platform creates the project repositories"
  type        = string
}

variable "github_app_id" {
  description = "ID of the GitHub App used by the platform"
  type        = string
}

variable "github_app_installation_id" {
  description = "Installation ID of the GitHub App in the organization"
  type        = string
}

variable "scaffolder_provider" {
  description = "Who writes the Terraform of the projects: bedrock or anthropic"
  type        = string
  default     = "anthropic"
}

variable "anthropic_model" {
  description = "Model used by the chat agent"
  type        = string
  default     = "claude-haiku-4-5-20251001"
}