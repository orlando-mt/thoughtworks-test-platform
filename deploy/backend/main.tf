data "aws_caller_identity" "current" {}

# Lo que creó el repo de infra: red, cluster, ECR, ALB y base de datos
data "terraform_remote_state" "infra" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "thoughtworks-test-infra/${var.environment}.tfstate"
    region = var.region
  }
}

data "aws_vpc" "this" {
  id = local.infra.vpc_id
}

locals {
  infra = data.terraform_remote_state.infra.outputs
  name  = "${var.name_prefix}-${var.environment}"

  # Secretos de la aplicación, creados a mano en Parameter Store (SecureString)
  ssm_prefix = "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/${var.name_prefix}/${var.environment}"

  tags = {
    Project     = var.name_prefix
    Environment = var.environment
    ManagedBy   = "terraform"
    Repository  = "thoughtworks-test-platform"
  }
}

# API: no es pública. La web la alcanza como http://api:8080 por Service Connect
module "api" {
  source = "github.com/orlando-mt/terraform-aws-ecs-service?ref=v1.0.0"

  service_name = "${local.name}-api"
  cluster_name = local.infra.ecs_cluster_name
  cluster_arn  = local.infra.ecs_cluster_arn

  cpu_architecture   = "X86_64"
  task_cpu           = 512
  task_memory        = 2048
  desired_count      = 1
  enable_autoscaling = false

  containers = {
    api = {
      image = "${local.infra.ecr_repository_urls["api"]}:${var.image_tag}"

      port_mappings = [
        { container_port = 8080, name = "http", app_protocol = "http" }
      ]

      environment = {
        DB_HOST                    = local.infra.db_endpoint
        DB_PORT                    = "5432"
        DB_NAME                    = local.infra.db_name
        DB_USER                    = "platform"
        JWT_TTL_MINUTES            = "60"
        SWAGGER_ENABLED            = "false"
        GITHUB_ORG                 = var.github_org
        GITHUB_APP_ID              = var.github_app_id
        GITHUB_APP_INSTALLATION_ID = var.github_app_installation_id
        SCAFFOLDER_PROVIDER        = var.scaffolder_provider
        TF_STATE_BUCKET            = var.state_bucket
        AWS_REGION                 = var.region
      }

      secrets = {
        # La contraseña la gestiona RDS en Secrets Manager; se toma la clave "password" del JSON
        DB_PASSWORD            = "${local.infra.db_master_user_secret_arn}:password::"
        JWT_SECRET             = "${local.ssm_prefix}/jwt-secret"
        GITHUB_APP_PRIVATE_KEY = "${local.ssm_prefix}/github-app-private-key"
        ANTHROPIC_API_KEY      = "${local.ssm_prefix}/anthropic-api-key"
      }
    }
  }

  vpc_id           = local.infra.vpc_id
  subnet_ids       = local.infra.public_subnet_ids
  assign_public_ip = true # subredes públicas sin NAT: la tarea sale a internet con su IP pública

  container_port      = 8080
  ingress_cidr_blocks = [data.aws_vpc.this.cidr_block]
  create_target_group = false

  service_connect_namespace_arn = local.infra.service_connect_namespace_arn
  service_connect_services = {
    api = { port_name = "http", client_alias_port = 8080 }
  }

  log_retention_days = 7
  tags               = local.tags
}

# Web: única entrada pública, detrás del ALB
module "web" {
  source = "github.com/orlando-mt/terraform-aws-ecs-service?ref=v1.0.0"

  # La API debe estar registrada en Service Connect antes de que arranque la web
  depends_on = [module.api]

  service_name = "${local.name}-web"
  cluster_name = local.infra.ecs_cluster_name
  cluster_arn  = local.infra.ecs_cluster_arn

  cpu_architecture   = "X86_64"
  task_cpu           = 512
  task_memory        = 1024
  desired_count      = 1
  enable_autoscaling = false

  containers = {
    web = {
      image = "${local.infra.ecr_repository_urls["web"]}:${var.image_tag}"

      port_mappings = [
        { container_port = 3000, name = "http", app_protocol = "http" }
      ]

      environment = {
        PLATFORM_API_URL = "http://api:8080"
        ANTHROPIC_MODEL  = var.anthropic_model
        HOSTNAME         = "0.0.0.0"
        PORT             = "3000"
      }

      secrets = {
        ANTHROPIC_API_KEY = "${local.ssm_prefix}/anthropic-api-key"
      }
    }
  }

  vpc_id           = local.infra.vpc_id
  subnet_ids       = local.infra.public_subnet_ids
  assign_public_ip = true

  container_port             = 3000
  ingress_security_group_ids = [local.infra.alb_security_group_id]

  listener_arn                = local.infra.alb_listener_arn
  listener_rule_priority      = 100
  listener_rule_path_patterns = ["/*"]
  health_check_path           = "/login"

  # Solo como cliente: sin servicios publicados
  service_connect_namespace_arn = local.infra.service_connect_namespace_arn

  log_retention_days = 7
  tags               = local.tags
}