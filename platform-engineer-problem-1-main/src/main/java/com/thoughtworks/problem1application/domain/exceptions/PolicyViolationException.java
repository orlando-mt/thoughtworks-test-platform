// PolicyViolationException.java
package com.thoughtworks.problem1application.domain.exceptions;

public class PolicyViolationException extends RuntimeException {

    private final String policy;

    public PolicyViolationException(String policy, String reason) {
        super(reason);
        this.policy = policy;
    }

    public String getPolicy() {
        return policy;
    }
}