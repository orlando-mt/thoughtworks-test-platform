package com.thoughtworks.problem1application.domain.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid e-mail or password.");
    }
}