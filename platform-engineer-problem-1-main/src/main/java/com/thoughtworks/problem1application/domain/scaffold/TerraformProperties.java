package com.thoughtworks.problem1application.domain.scaffold;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("platform.terraform")
public record TerraformProperties(String region, String stateBucket, String awsProviderVersion) {
}