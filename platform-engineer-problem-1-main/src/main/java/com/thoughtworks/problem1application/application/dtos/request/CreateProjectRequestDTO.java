// application/dtos/request/CreateProjectRequestDTO.java
package com.thoughtworks.problem1application.application.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequestDTO(@NotBlank String name) {
}