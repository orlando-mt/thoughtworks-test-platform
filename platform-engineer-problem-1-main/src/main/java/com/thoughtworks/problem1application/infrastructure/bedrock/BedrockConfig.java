package com.thoughtworks.problem1application.infrastructure.bedrock;

import com.thoughtworks.problem1application.domain.scaffold.TerraformProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
@EnableConfigurationProperties({BedrockProperties.class, TerraformProperties.class})
public class BedrockConfig {

    /**
     * En local usa el perfil de AWS_PROFILE (el .env lo lee Spring, no el SDK, por eso se pasa explícito).
     * En ECS el perfil va vacío y la cadena por defecto toma el rol de la tarea.
     */
    @Bean
    BedrockRuntimeClient bedrockRuntimeClient(BedrockProperties props) {
        AwsCredentialsProvider credentials = StringUtils.hasText(props.profile())
                ? ProfileCredentialsProvider.builder().profileName(props.profile()).build()
                : DefaultCredentialsProvider.builder().build();
        return BedrockRuntimeClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .overrideConfiguration(o -> o.apiCallTimeout(props.timeout()))
                .build();
    }
}