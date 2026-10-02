package com.thoughtworks.problem1application.domain.exceptions;

public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(Throwable cause) {
        super("The platform catalog is not available right now.", cause);
    }
}