package com.thoughtworks.problem1application.unit.domain.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.thoughtworks.problem1application.domain.scaffold.TfvarsRenderer;

class TfvarsRendererTest {

    @Test
    void rendersOneMapPerModuleKeyedByResourceName() {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("versioning_enabled", true);
        inputs.put("tier", "standard");
        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();
        buckets.put("pagos-dev-reportes-a1b2c3", inputs);
        buckets.put("pagos-dev-logs-d4e5f6", Map.of());
        Map<String, Map<String, Map<String, Object>>> byVariable = new LinkedHashMap<>();
        byVariable.put("s3_bucket", buckets);

        assertThat(TfvarsRenderer.render("dev", byVariable)).isEqualTo("""
                # Managed by the Kordanix platform: regenerated from its database on every request.
                environment = "dev"

                s3_bucket = {
                  "pagos-dev-reportes-a1b2c3" = {
                    versioning_enabled = true
                    tier               = "standard"
                  }
                  "pagos-dev-logs-d4e5f6" = {}
                }
                """);
    }
}