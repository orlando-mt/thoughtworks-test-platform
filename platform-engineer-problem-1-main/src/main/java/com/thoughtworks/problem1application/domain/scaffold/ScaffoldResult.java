package com.thoughtworks.problem1application.domain.scaffold;

import java.util.Map;

public record ScaffoldResult(Map<String, String> files, int attempts) {
}