package com.thoughtworks.problem1application.unit.domain.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.thoughtworks.problem1application.domain.scaffold.ScaffoldModule;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldRequestFactory;

class ScaffoldRequestFactoryTest {

    @Test
    void rendersCatalogValuesAsHclLiterals() {
        assertThat(ScaffoldRequestFactory.hclLiteral(true)).isEqualTo("true");
        assertThat(ScaffoldRequestFactory.hclLiteral(30)).isEqualTo("30");
        assertThat(ScaffoldRequestFactory.hclLiteral("IMMUTABLE")).isEqualTo("\"IMMUTABLE\"");
        assertThat(ScaffoldRequestFactory.hclLiteral("a\"b")).isEqualTo("\"a\\\"b\"");
        assertThat(ScaffoldRequestFactory.hclLiteral(null)).isEqualTo("null");
    }

    @Test
    void turnsCatalogIdsIntoTerraformLabels() {
        assertThat(ScaffoldModule.labelOf("s3-bucket")).isEqualTo("s3_bucket");
        assertThat(ScaffoldModule.labelOf("ECR-Repository")).isEqualTo("ecr_repository");
    }
}