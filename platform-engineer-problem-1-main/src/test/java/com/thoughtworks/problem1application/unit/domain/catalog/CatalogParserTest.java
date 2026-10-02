package com.thoughtworks.problem1application.unit.domain.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.thoughtworks.problem1application.domain.catalog.CatalogParser;
import com.thoughtworks.problem1application.domain.catalog.CatalogSource;
import com.thoughtworks.problem1application.domain.catalog.InputType;
import com.thoughtworks.problem1application.domain.catalog.ModuleDefinition;
import com.thoughtworks.problem1application.domain.exceptions.InvalidCatalogException;

class CatalogParserTest {

    private final CatalogParser parser = new CatalogParser();

    @Test
    void shouldParseRealCatalog() throws IOException {
        List<ModuleDefinition> modules = parser.parse(source(resource("catalog/catalog.yml")));

        ModuleDefinition s3 = modules.getFirst();
        assertEquals("s3-bucket", s3.id());
        assertEquals("bucket_name", s3.nameVariable());
        assertEquals(1, s3.inputs().size());
        assertEquals(InputType.BOOLEAN, s3.inputs().getFirst().type());
        assertEquals(true, s3.inputs().getFirst().defaultValue());
        assertEquals(true, s3.enforced().get("block_public_access"));
        assertEquals(2, s3.outputs().size());
    }

    @Test
    void shouldRejectComplexTypeInAsk() throws IOException {
        String catalog = resource("catalog/catalog.yml").replace("- versioning_enabled", "- lifecycle_rules");

        InvalidCatalogException ex = assertThrows(InvalidCatalogException.class, () -> parser.parse(source(catalog)));

        assertTrue(ex.getProblems().getFirst().contains("lifecycle_rules"));
    }

    @Test
    void shouldRejectUncoveredRequiredInput() throws IOException {
        String catalog = resource("catalog/catalog.yml").replace("name_variable: bucket_name",
                "name_variable: website_index_document");

        InvalidCatalogException ex = assertThrows(InvalidCatalogException.class, () -> parser.parse(source(catalog)));

        assertTrue(ex.getProblems().stream().anyMatch(p -> p.contains("required input 'bucket_name'")));
    }

    @Test
    void shouldRejectUnknownFixedInput() throws IOException {
        String catalog = resource("catalog/catalog.yml").replace("force_destroy: false", "make_it_free: true");

        InvalidCatalogException ex = assertThrows(InvalidCatalogException.class, () -> parser.parse(source(catalog)));

        assertTrue(ex.getProblems().stream().anyMatch(p -> p.contains("make_it_free")));
    }

    private CatalogSource source(String catalogYaml) throws IOException {
        Map<String, String> files = Map.of(
                "catalog.yml", catalogYaml,
                "generated/s3-bucket.json", resource("catalog/generated/s3-bucket.json"));
        return files::get;
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = CatalogParserTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}