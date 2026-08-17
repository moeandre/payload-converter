package io.payloadconverter.mapping.model;

import java.util.Map;

/**
 * Especificacao de uma funcao de transformacao a aplicar sobre o valor de uma regra.
 *
 * @param function nome da funcao registrada (ex: "map", "concat", "dataFormato")
 * @param args     argumentos nomeados especificos da funcao
 */
public record TransformSpec(String function, Map<String, Object> args) {
    public TransformSpec {
        if (function == null || function.isBlank()) {
            throw new IllegalArgumentException("Especificacao de 'transform' sem 'function'");
        }
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
