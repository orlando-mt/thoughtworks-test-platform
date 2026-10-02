package com.thoughtworks.problem1application.api.handlers;

import java.util.NoSuchElementException;

import com.thoughtworks.problem1application.domain.exceptions.*;
import com.thoughtworks.problem1application.domain.scaffold.ScaffoldGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleUserNotFound(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found.");
    }

    @ExceptionHandler(InvalidResourceRequestException.class)
    public ProblemDetail handleInvalidResourceRequest(InvalidResourceRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("errors", ex.getErrors());
        return problem;
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ProblemDetail handlePolicyViolation(PolicyViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("policy", ex.getPolicy());
        return problem;
    }

    @ExceptionHandler(ModuleNotFoundException.class)
    public ProblemDetail handleModuleNotFound(ModuleNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProjectAlreadyExistsException.class)
    public ProblemDetail handleProjectAlreadyExists(ProjectAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** GitHub respondió con error: no es culpa del usuario ni de la API, es de la dependencia. */
    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleGitHubError(RestClientResponseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "GitHub rejected the request (" + ex.getStatusCode().value() + ").");
        problem.setProperty("upstreamBody", ex.getResponseBodyAsString());
        return problem;
    }

    /** El modelo no logró un Terraform válido (o Bedrock falló): es de la dependencia, no del usuario. */
    @ExceptionHandler(ScaffoldGenerationException.class)
    public ProblemDetail handleScaffoldGeneration(ScaffoldGenerationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setProperty("errors", ex.getErrors());
        return problem;
    }
}