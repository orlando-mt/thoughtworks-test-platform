package com.thoughtworks.problem1application.domain.catalog;

public record ModuleInput(String name, InputType type, String description, Object defaultValue, boolean required) {
}