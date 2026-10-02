// ProjectAlreadyExistsException.java
package com.thoughtworks.problem1application.domain.exceptions;

public class ProjectAlreadyExistsException extends RuntimeException {
    public ProjectAlreadyExistsException(String message) {
        super(message);
    }
}