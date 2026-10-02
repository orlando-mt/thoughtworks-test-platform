package com.thoughtworks.problem1application.domain.scaffold;

import java.util.List;
import java.util.Map;

/** Puerto hacia el modelo que escribe el Terraform. La implementación real es Bedrock. */
public interface ModuleScaffolder {

    /**
     * @param previousErrors vacío en el primer intento; luego, lo que rechazó el validador
     * @return ruta del archivo → contenido
     */
    Map<String, String> generate(ScaffoldRequest request, List<String> previousErrors);
}