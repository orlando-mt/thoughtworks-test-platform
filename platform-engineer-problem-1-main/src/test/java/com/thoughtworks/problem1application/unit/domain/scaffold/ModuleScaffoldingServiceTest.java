package com.thoughtworks.problem1application.unit.domain.scaffold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffolder;
import com.thoughtworks.problem1application.domain.scaffold.ModuleScaffoldingService;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldGenerationException;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequest;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequestFactory;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldResult;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldValidator;
import com.thoughtworks.problem1application.helpers.ScaffoldFixtures;

@ExtendWith(MockitoExtension.class)
class ModuleScaffoldingServiceTest {

    @Mock
    private ScaffoldRequestFactory requestFactory;

    @Mock
    private ModuleScaffolder scaffolder;

    private final ScaffoldRequest request = ScaffoldFixtures.request();
    private ModuleScaffoldingService service;

    @BeforeEach
    void setUp() {
        service = new ModuleScaffoldingService(requestFactory, scaffolder, new ScaffoldValidator(), 2);
        when(requestFactory.build(eq("pagos"), eq("pagos-infra"), any())).thenReturn(request);
    }

    @Test
    void returnsTheFilesWhenTheFirstAttemptIsValid() {
        when(scaffolder.generate(request, List.of())).thenReturn(ScaffoldFixtures.validFiles());

        ScaffoldResult result = service.scaffold("pagos", "pagos-infra", Set.of("s3-bucket"));

        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.files()).containsOnlyKeys(request.expectedPaths());
    }

    @Test
    void sendsTheValidatorErrorsBackToTheModel() {
        Map<String, String> invalid = ScaffoldFixtures.validFiles();
        invalid.computeIfPresent("main.tf", (path, content) ->
                content.replace("force_destroy       = false", "force_destroy       = true"));
        when(scaffolder.generate(request, List.of())).thenReturn(invalid);
        when(scaffolder.generate(request,
                List.of("main.tf: module \"s3_bucket\" must set force_destroy = false (platform policy)")))
                .thenReturn(ScaffoldFixtures.validFiles());

        ScaffoldResult result = service.scaffold("pagos", "pagos-infra", Set.of("s3-bucket"));

        assertThat(result.attempts()).isEqualTo(2);
    }

    @Test
    void failsAfterMaxAttempts() {
        when(scaffolder.generate(eq(request), anyList())).thenReturn(Map.of());

        assertThatThrownBy(() -> service.scaffold("pagos", "pagos-infra", Set.of("s3-bucket")))
                .isInstanceOf(ScaffoldGenerationException.class)
                .satisfies(e -> assertThat(((ScaffoldGenerationException) e).getErrors())
                        .contains("Missing file: main.tf"));
        verify(scaffolder, times(2)).generate(eq(request), anyList());
    }
}