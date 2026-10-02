package com.thoughtworks.problem1application.domain.scaffold;

import java.util.List;

public class ScaffoldGenerationException extends RuntimeException {

    private final List<String> errors;

    public ScaffoldGenerationException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}