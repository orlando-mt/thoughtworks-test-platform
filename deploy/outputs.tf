output "url" {
  description = "Public address of the platform"
  value       = "http://${local.infra.alb_dns_name}"
}

output "api_log_group" {
  description = "CloudWatch log group of the API"
  value       = module.api.log_group_name
}

output "web_log_group" {
  description = "CloudWatch log group of the web"
  value       = module.web.log_group_name
}