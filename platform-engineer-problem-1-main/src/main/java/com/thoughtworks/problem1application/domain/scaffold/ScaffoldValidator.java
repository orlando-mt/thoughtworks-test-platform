package com.thoughtworks.problem1application.domain.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import static java.util.regex.Pattern.quote;

/**
 * Revisa lo que devolvió el modelo antes de que llegue al repo.
 * Cada error vuelve al modelo como feedback en el siguiente intento.
 */
@Component
public class ScaffoldValidator {

    static final int MAX_FILE_CHARS = 32_000;

    private static final Pattern MODULE_HEADER = Pattern.compile("(?m)^\\s*module\\s+\"([^\"]+)\"");
    private static final Pattern SOURCE = Pattern.compile("\\bsource\\s*=\\s*\"([^\"]*)\"");

    private record Rule(Pattern pattern, String message) {
    }

    private static final List<Rule> FORBIDDEN = List.of(
            new Rule(Pattern.compile("(?m)^\\s*resource\\s+\""),
                    "resource blocks are not allowed; only catalog modules create infrastructure"),
            new Rule(Pattern.compile("(?m)^\\s*data\\s+\""), "data sources are not allowed"),
            new Rule(Pattern.compile("\\bprovisioner\\s+\""), "provisioners are not allowed"),
            new Rule(Pattern.compile("```"), "markdown fences are not allowed; return the raw file content"));

    public List<String> validate(ScaffoldRequest request, Map<String, String> files) {
        List<String> errors = new ArrayList<>();
        checkPaths(request, files, errors);
        files.forEach((path, content) -> checkFile(path, content, errors));

        String main = files.getOrDefault("main.tf", "");
        String variables = files.getOrDefault("variables.tf", "");
        String outputs = files.getOrDefault("outputs.tf", "");

        checkOnlyCatalogModules(request, main, errors);
        require(variables, "(?m)^\\s*variable\\s+\"environment\"\\s*\\{",
                "variables.tf: missing variable \"environment\"", errors);
        for (ScaffoldModule module : request.modules()) {
            checkModuleBlock(module, main, errors);
            checkModuleVariable(module, variables, errors);
            checkModuleOutputs(module, outputs, errors);
        }
        checkProvider(request, files.getOrDefault("provider.tf", ""), errors);
        checkBackend(request, files, errors);
        return errors;
    }

    private void checkPaths(ScaffoldRequest request, Map<String, String> files, List<String> errors) {
        Set<String> expected = new LinkedHashSet<>(request.expectedPaths());
        expected.stream()
                .filter(path -> !files.containsKey(path))
                .forEach(path -> errors.add("Missing file: " + path));
        files.keySet().stream()
                .filter(path -> !expected.contains(path))
                .forEach(path -> errors.add("Unexpected file: " + path + " (allowed: " + String.join(", ", expected) + ")"));
    }

    private void checkFile(String path, String content, List<String> errors) {
        if (content.length() > MAX_FILE_CHARS) {
            errors.add(path + ": file is too large");
        }
        FORBIDDEN.stream()
                .filter(rule -> rule.pattern().matcher(content).find())
                .forEach(rule -> errors.add(path + ": " + rule.message()));
        if (!"main.tf".equals(path) && MODULE_HEADER.matcher(content).find()) {
            errors.add(path + ": module blocks must live in main.tf");
        }
    }

    private void checkOnlyCatalogModules(ScaffoldRequest request, String main, List<String> errors) {
        Set<String> labels = request.modules().stream().map(ScaffoldModule::label).collect(Collectors.toSet());
        Set<String> sources = request.modules().stream().map(ScaffoldModule::source).collect(Collectors.toSet());
        Matcher modules = MODULE_HEADER.matcher(main);
        while (modules.find()) {
            if (!labels.contains(modules.group(1))) {
                errors.add("main.tf: module \"" + modules.group(1) + "\" was not requested");
            }
        }
        Matcher used = SOURCE.matcher(main);
        while (used.find()) {
            if (!sources.contains(used.group(1))) {
                errors.add("main.tf: source \"" + used.group(1) + "\" is not allowed; use the exact catalog source");
            }
        }
    }

    private void checkModuleBlock(ScaffoldModule module, String main, List<String> errors) {
        String label = module.label();
        Optional<String> block = HclBlocks.find(main, "module", label);
        if (block.isEmpty()) {
            errors.add("main.tf: missing module \"" + label + "\"");
            return;
        }
        String body = block.get();
        String where = "main.tf: module \"" + label + "\" ";
        require(body, "\\bsource\\s*=\\s*\"" + quote(module.source()) + "\"",
                where + "must use source = \"" + module.source() + "\"", errors);
        require(body, "\\bfor_each\\s*=\\s*var\\." + quote(label) + "\\b",
                where + "must use for_each = var." + label, errors);
        require(body, "\\b" + quote(module.nameVariable()) + "\\s*=\\s*each\\.key\\b",
                where + "must set " + module.nameVariable() + " = each.key", errors);
        for (String input : module.inputs()) {
            require(body, "\\b" + quote(input) + "\\s*=\\s*each\\.value\\." + quote(input) + "\\b",
                    where + "must set " + input + " = each.value." + input, errors);
        }
        module.fixed().forEach((name, literal) ->
                require(body, "\\b" + quote(name) + "\\s*=\\s*" + quote(literal) + "(?=\\s|$)",
                        where + "must set " + name + " = " + literal + " (platform policy)", errors));
        if (module.tagsVariable() != null) {
            require(body, "\\b" + quote(module.tagsVariable()) + "\\s*=",
                    where + "must set " + module.tagsVariable(), errors);
        }
    }

    private void checkModuleVariable(ScaffoldModule module, String variables, List<String> errors) {
        String label = module.label();
        Optional<String> block = HclBlocks.find(variables, "variable", label);
        if (block.isEmpty()) {
            errors.add("variables.tf: missing variable \"" + label + "\"");
            return;
        }
        String body = block.get();
        String where = "variables.tf: variable \"" + label + "\" ";
        require(body, "\\bmap\\s*\\(\\s*object\\s*\\(", where + "must be map(object({...}))", errors);
        for (String input : module.inputs()) {
            require(body, "\\b" + quote(input) + "\\s*=", where + "must declare attribute " + input, errors);
        }
        require(body, "\\bdefault\\s*=\\s*\\{\\s*\\}", where + "must have default = {}", errors);
    }

    private void checkModuleOutputs(ScaffoldModule module, String outputs, List<String> errors) {
        for (String output : module.outputs()) {
            String name = module.label() + "_" + output;
            Optional<String> block = HclBlocks.find(outputs, "output", name);
            if (block.isEmpty()) {
                errors.add("outputs.tf: missing output \"" + name + "\"");
                continue;
            }
            require(block.get(), "\\bmodule\\." + quote(module.label()) + "\\b.*\\." + quote(output) + "\\b",
                    "outputs.tf: output \"" + name + "\" must read " + output + " from module." + module.label(), errors);
        }
    }

    private void checkProvider(ScaffoldRequest request, String provider, List<String> errors) {
        require(provider, "(?m)^\\s*provider\\s+\"aws\"\\s*\\{", "provider.tf: missing provider \"aws\"", errors);
        require(provider, "\\bregion\\s*=\\s*\"" + quote(request.region()) + "\"",
                "provider.tf: region must be \"" + request.region() + "\"", errors);
        require(provider, "\"hashicorp/aws\"", "provider.tf: required_providers must declare hashicorp/aws", errors);
    }

    private void checkBackend(ScaffoldRequest request, Map<String, String> files, List<String> errors) {
        require(files.getOrDefault("backend.tf", ""), "\\bbackend\\s+\"s3\"\\s*\\{\\s*\\}",
                "backend.tf: must declare an empty partial backend \"s3\" {}", errors);
        for (String env : request.environments()) {
            String path = ScaffoldRequest.backendConfigPath(env);
            String hcl = files.get(path);
            if (hcl == null) {
                continue;
            }
            require(hcl, "\\bbucket\\s*=\\s*\"" + quote(request.stateBucket()) + "\"",
                    path + ": bucket must be \"" + request.stateBucket() + "\"", errors);
            require(hcl, "\\bkey\\s*=\\s*\"" + quote(request.stateKey(env)) + "\"",
                    path + ": key must be \"" + request.stateKey(env) + "\"", errors);
            require(hcl, "\\bregion\\s*=\\s*\"" + quote(request.region()) + "\"",
                    path + ": region must be \"" + request.region() + "\"", errors);
            require(hcl, "\\buse_lockfile\\s*=\\s*true\\b", path + ": use_lockfile must be true", errors);
        }
    }

    private static void require(String text, String regex, String error, List<String> errors) {
        if (!Pattern.compile(regex).matcher(text).find()) {
            errors.add(error);
        }
    }
}