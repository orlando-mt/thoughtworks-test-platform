package com.thoughtworks.problem1application.domain.catalog;

/** Fuente de los archivos del catálogo (catalog.yml y generated/*.json). */
@FunctionalInterface
public interface CatalogSource {

    String read(String path);
}