package com.thoughtworks.problem1application.domain.exceptions;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(String id) {
        super("Module '" + id + "' is not published in the platform catalog.");
    }
}