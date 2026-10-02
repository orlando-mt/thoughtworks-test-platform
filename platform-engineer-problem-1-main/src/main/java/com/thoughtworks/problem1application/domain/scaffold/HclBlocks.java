package com.thoughtworks.problem1application.domain.scaffold;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrae el cuerpo de un bloque HCL ({@code module "x" { ... }}) contando llaves fuera de strings. */
final class HclBlocks {

    private HclBlocks() {
    }

    static Optional<String> find(String text, String type, String label) {
        Matcher header = Pattern.compile("(?m)^\\s*" + type + "\\s+\"" + Pattern.quote(label) + "\"\\s*\\{")
                .matcher(text);
        if (!header.find()) {
            return Optional.empty();
        }
        int depth = 1;
        boolean inString = false;
        for (int i = header.end(); i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return Optional.of(text.substring(header.end(), i));
            }
        }
        return Optional.empty();
    }
}