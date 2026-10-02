// ProjectNotFoundException.java
package com.thoughtworks.problem1application.domain.exceptions;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String name) {
        super("Project '" + name + "' not found.");
    }
}