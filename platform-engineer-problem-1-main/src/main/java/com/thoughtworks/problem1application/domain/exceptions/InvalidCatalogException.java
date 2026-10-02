package com.thoughtworks.problem1application.domain.exceptions;

import java.util.List;

public class InvalidCatalogException extends RuntimeException {

    private final List<String> problems;

    public InvalidCatalogException(List<String> problems) {
        super("Invalid platform catalog: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}