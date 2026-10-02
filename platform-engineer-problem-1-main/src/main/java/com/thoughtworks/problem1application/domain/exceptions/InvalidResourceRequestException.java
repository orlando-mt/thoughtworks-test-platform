package com.thoughtworks.problem1application.domain.exceptions;

import java.util.List;

public class InvalidResourceRequestException extends RuntimeException {

    private final List<String> errors;

    public InvalidResourceRequestException(List<String> errors) {
        super("Invalid resource request.");
        this.errors = List.copyOf(errors);
    }

    public InvalidResourceRequestException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}