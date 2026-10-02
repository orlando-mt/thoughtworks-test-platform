package com.thoughtworks.problem1application.domain.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("E-mail already registered.");
    }
}